package com.fitness.camera;

import com.fitness.config.AppConfig;
import com.fitness.pose.RawFrame;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the webcam and a single dedicated capture thread, implementing the
 * "Camera Thread -&gt; Frame Queue" half of the pipeline described in the
 * project README's Threading section. Frames are pushed into a small
 * bounded queue; if the consumer (the pose/ML thread) falls behind, the
 * <em>oldest</em> queued frame is dropped to make room for the newest one,
 * so the app always processes close-to-live video instead of catching up
 * through a backlog (the "frame skipping" behaviour called for by the spec).
 *
 * <p>Never touches JavaFX - the UI layer only ever sees {@link RawFrame}s
 * pulled from {@link #pollFrame(long)}, or a plain-text status string via
 * {@link #setStatusListener(Consumer)} for camera lifecycle events
 * (opened, disconnected, reconnecting, gave up).</p>
 */
public final class CameraManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(CameraManager.class.getName());
    private static final int MAX_CONSECUTIVE_READ_FAILURES = 15;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_BACKOFF_MS = 1000;
    private static final int PROBE_MAX_INDEX = 5;

    private final AppConfig config;
    private final BlockingQueue<RawFrame> frameQueue;
    private volatile Consumer<String> statusListener = s -> { };

    private ExecutorService captureExecutor;
    private VideoCapture capture;
    private volatile boolean running = false;

    public CameraManager(AppConfig config) {
        this.config = config;
        this.frameQueue = new ArrayBlockingQueue<>(Math.max(1, config.getFrameQueueSize()));
    }

    public void setStatusListener(Consumer<String> listener) {
        this.statusListener = listener != null ? listener : (s -> { });
    }

    /**
     * Best-effort webcam discovery: OpenCV has no portable "list all camera
     * devices" API, so this probes indices {@code 0..maxIndex-1} by briefly
     * opening and immediately releasing each one. Slow (each probe can take
     * up to ~1s on some platforms/drivers) - intended for a one-off
     * "refresh camera list" action in Settings, not for frequent polling.
     */
    public List<Integer> discoverCameras() {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < PROBE_MAX_INDEX; i++) {
            VideoCapture probe = new VideoCapture(i);
            try {
                if (probe.isOpened()) {
                    found.add(i);
                }
            } finally {
                probe.release();
            }
        }
        return found;
    }

    /** Opens the configured camera and starts the capture thread. */
    public synchronized void start() throws CameraException {
        if (running) {
            return;
        }
        openCapture(config.getCameraIndex());
        running = true;
        captureExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "camera-capture");
            t.setDaemon(true);
            return t;
        });
        captureExecutor.submit(this::captureLoop);
        statusListener.accept("Camera started");
    }

    private void openCapture(int index) throws CameraException {
        capture = new VideoCapture(index);
        if (!capture.isOpened()) {
            capture.release();
            throw new CameraException("Could not open camera at index " + index
                    + ". Check that a webcam is connected, not in use by another application, "
                    + "and that camera permission has been granted to this app.");
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, config.getCameraWidth());
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, config.getCameraHeight());
        capture.set(Videoio.CAP_PROP_FPS, config.getCameraFps());
    }

    private void captureLoop() {
        Mat mat = new Mat();
        int consecutiveFailures = 0;

        while (running) {
            boolean ok;
            try {
                ok = capture.read(mat);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Camera read() threw unexpectedly", e);
                ok = false;
            }

            if (!ok || mat.empty()) {
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_READ_FAILURES) {
                    handleDisconnect();
                    consecutiveFailures = 0;
                }
                continue;
            }
            consecutiveFailures = 0;

            RawFrame frame = matToRawFrame(mat);
            if (frame != null) {
                offerDroppingOldest(frame);
            }
        }
        mat.release();
    }

    private void handleDisconnect() {
        statusListener.accept("Camera disconnected - attempting to reconnect...");
        for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS && running; attempt++) {
            try {
                Thread.sleep(RECONNECT_BACKOFF_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                capture.release();
                openCapture(config.getCameraIndex());
                statusListener.accept("Camera reconnected");
                return;
            } catch (CameraException e) {
                statusListener.accept("Reconnect attempt " + attempt + "/" + MAX_RECONNECT_ATTEMPTS + " failed");
            }
        }
        if (running) {
            statusListener.accept("Could not reconnect to the camera. Stop and press Start Workout to try again.");
            running = false;
        }
    }

    private void offerDroppingOldest(RawFrame frame) {
        if (!frameQueue.offer(frame)) {
            frameQueue.poll();
            frameQueue.offer(frame);
        }
    }

    private RawFrame matToRawFrame(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();
        if (channels != 3) {
            LOG.warning("Unexpected frame format (channels=" + channels + "), skipping frame");
            return null;
        }
        byte[] data = new byte[width * height * 3];
        mat.get(0, 0, data);
        return new RawFrame(width, height, data, System.nanoTime());
    }

    /** Blocks up to {@code timeoutMs} for the next captured frame, or returns null on timeout. */
    public RawFrame pollFrame(long timeoutMs) throws InterruptedException {
        return frameQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void restart() throws CameraException {
        stop();
        start();
    }

    public synchronized void stop() {
        running = false;
        if (captureExecutor != null) {
            captureExecutor.shutdownNow();
            try {
                captureExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            captureExecutor = null;
        }
        if (capture != null) {
            capture.release();
            capture = null;
        }
        frameQueue.clear();
        statusListener.accept("Camera stopped");
    }

    @Override
    public void close() {
        stop();
    }
}
