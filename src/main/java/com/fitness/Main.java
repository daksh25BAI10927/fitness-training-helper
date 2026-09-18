package com.fitness;

import com.fitness.camera.CameraManager;
import com.fitness.camera.CameraException;
import com.fitness.config.AppConfig;
import com.fitness.config.ConfigManager;
import com.fitness.database.DatabaseException;
import com.fitness.database.DatabaseManager;
import com.fitness.pose.OnnxMoveNetPoseEstimator;
import com.fitness.pose.PoseEstimationException;
import com.fitness.pose.PoseEstimator;
import com.fitness.pose.PoseResult;
import com.fitness.pose.RawFrame;
import com.fitness.ui.HistoryView;
import com.fitness.ui.MainView;
import com.fitness.ui.SettingsView;
import com.fitness.utils.AppLogger;
import com.fitness.utils.ImageConverter;
import com.fitness.workout.WorkoutManager;
import com.fitness.workout.WorkoutSession;
import com.fitness.workout.WorkoutState;
import com.fitness.workout.WorkoutTick;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the AI Real-Time Workout &amp; Movement Tracker.
 *
 * <p>Extends {@link Application} to serve as the JavaFX application lifecycle
 * owner. Wires together the camera capture, ML inference, movement analysis,
 * and UI rendering pipelines on the correct threads:</p>
 * <ul>
 *   <li><b>Camera Thread</b> — owned by {@link CameraManager}, captures frames into a bounded queue</li>
 *   <li><b>Processing Thread</b> — polls frames, runs pose estimation and movement analysis</li>
 *   <li><b>JavaFX Application Thread</b> — renders camera preview, skeleton overlay, and stats</li>
 * </ul>
 */
public class Main extends Application {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private AppConfig config;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private CameraManager cameraManager;
    private PoseEstimator poseEstimator;
    private WorkoutManager workoutManager;
    private MainView mainView;

    private Thread processingThread;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private long sessionStartEpochMillis;
    private PoseResult lastPoseResult;

    @Override
    public void start(Stage primaryStage) {
        AppLogger.configure();
        LOG.info("Starting AI Workout Tracker...");

        // Load OpenCV native libraries
        try {
            nu.pattern.OpenCV.loadLocally();
            LOG.info("OpenCV loaded successfully.");
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Failed to load OpenCV native libraries", t);
            showError("OpenCV Error",
                    "Could not load OpenCV native libraries.\n\n"
                            + "Make sure the org.openpnp:opencv dependency is on the classpath.\n\n"
                            + "Error: " + t.getMessage());
            Platform.exit();
            return;
        }

        // Load configuration
        configManager = new ConfigManager();
        config = configManager.load();

        // Initialize database
        String dbPath = resolveDbPath(config.getDatabasePath());
        try {
            databaseManager = new DatabaseManager(dbPath);
        } catch (DatabaseException e) {
            LOG.log(Level.SEVERE, "Database initialization failed", e);
            showError("Database Error", "Could not initialize workout history database:\n" + e.getMessage());
            Platform.exit();
            return;
        }

        // Resolve and load pose estimation model
        String modelPath = resolveModelPath(config.getModelPath());
        if (modelPath != null) {
            try {
                poseEstimator = new OnnxMoveNetPoseEstimator(modelPath, config.getModelInputSize());
                LOG.info("Pose model loaded: " + modelPath);
            } catch (PoseEstimationException e) {
                LOG.log(Level.SEVERE, "Pose model loading failed", e);
                showError("Model Error",
                        "Could not load pose estimation model:\n" + e.getMessage()
                                + "\n\nThe application will run without pose detection.\n"
                                + "See src/main/resources/models/README.md for download instructions.");
                poseEstimator = null;
            }
        } else {
            LOG.warning("Pose model file not found. Running without pose detection.");
            showInfo("Model Not Found",
                    "The MoveNet ONNX model was not found.\n\n"
                            + "The camera preview will work, but pose detection and rep counting are disabled.\n\n"
                            + "To enable full functionality:\n"
                            + "1. Download movenet_singlepose_lightning.onnx\n"
                            + "2. Place it in src/main/resources/models/\n"
                            + "3. Restart the application.\n\n"
                            + "See src/main/resources/models/README.md for details.");
        }

        // Initialize camera manager
        cameraManager = new CameraManager(config);
        cameraManager.setStatusListener(status ->
                Platform.runLater(() -> {
                    if (mainView != null) {
                        mainView.setCameraStatus(status);
                    }
                }));

        // Build the UI
        mainView = new MainView(
                this::onStartWorkout,
                this::onPauseWorkout,
                this::onResumeWorkout,
                this::onStopWorkout,
                this::onResetWorkout,
                () -> onShowHistory(primaryStage),
                () -> onShowSettings(primaryStage)
        );

        Scene scene = new Scene(mainView.getRoot(), 960, 600);
        try {
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            LOG.warning("Could not load /css/style.css — running without custom styling.");
        }

        primaryStage.setTitle("AI Workout Tracker");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            shutdownAndExit(primaryStage);
        });
        primaryStage.show();

        // Start camera
        try {
            cameraManager.start();
            LOG.info("Camera started.");
        } catch (CameraException e) {
            LOG.log(Level.WARNING, "Camera failed to start", e);
            mainView.setCameraStatus("Camera unavailable: " + e.getMessage());
        }

        // Start the processing thread
        startProcessingThread();
    }

    // ---- Workout button handlers ----

    private void onStartWorkout() {
        workoutManager = new WorkoutManager(config);
        sessionStartEpochMillis = System.currentTimeMillis();
        workoutManager.start(System.nanoTime());
        LOG.info("Workout started.");
    }

    private void onPauseWorkout() {
        if (workoutManager != null) {
            workoutManager.pause();
            LOG.info("Workout paused.");
        }
    }

    private void onResumeWorkout() {
        if (workoutManager != null) {
            workoutManager.resume(System.nanoTime());
            LOG.info("Workout resumed.");
        }
    }

    private void onStopWorkout() {
        if (workoutManager != null) {
            workoutManager.stop();
            WorkoutSession session = workoutManager.buildSessionSummary(sessionStartEpochMillis);
            if (databaseManager != null) {
                try {
                    long sessionId = databaseManager.saveSession(session);
                    LOG.info("Workout session saved with id=" + sessionId);
                    showInfo("Workout Saved",
                            "Session saved!\n"
                                    + "Reps: " + session.getTotalReps()
                                    + "  Sets: " + session.getTotalSets()
                                    + "  Duration: " + formatDuration(session.getWorkoutDurationMs()));
                } catch (DatabaseException e) {
                    LOG.log(Level.SEVERE, "Failed to save workout session", e);
                    showError("Save Error", "Could not save workout session:\n" + e.getMessage());
                }
            }
            workoutManager = null;
        }
    }

    private void onResetWorkout() {
        if (workoutManager != null) {
            workoutManager.reset();
            LOG.info("Workout reset.");
        }
        workoutManager = null;
    }

    private void onShowHistory(Stage owner) {
        if (databaseManager != null) {
            HistoryView.show(owner, databaseManager);
        }
    }

    private void onShowSettings(Stage owner) {
        SettingsView.show(owner, configManager, config);
    }

    // ---- Processing thread ----

    private void startProcessingThread() {
        processing.set(true);
        processingThread = new Thread(() -> {
            int frameCount = 0;
            int inferenceInterval = Math.max(1, config.getInferenceEveryNFrames());

            while (processing.get()) {
                try {
                    RawFrame frame = cameraManager.pollFrame(100);
                    if (frame == null) {
                        continue;
                    }

                    // Convert to JavaFX image for camera preview
                    final Image fxImage = ImageConverter.toFxImage(frame);

                    // Run pose estimation on the configured interval
                    if (poseEstimator != null && frameCount % inferenceInterval == 0) {
                        try {
                            lastPoseResult = poseEstimator.estimate(frame);
                        } catch (PoseEstimationException e) {
                            LOG.log(Level.FINE, "Pose estimation failed for frame " + frameCount, e);
                            // Continue with last known pose or null
                        }
                    }

                    // Run movement analysis if workout is active
                    final WorkoutTick tick;
                    WorkoutManager wm = workoutManager; // local ref for thread safety
                    if (wm != null && wm.getState() == WorkoutState.ACTIVE && lastPoseResult != null) {
                        tick = wm.processFrame(lastPoseResult);
                    } else if (wm != null && wm.getState() == WorkoutState.PAUSED && lastPoseResult != null) {
                        // Still show skeleton while paused, but don't advance analysis
                        tick = null;
                    } else {
                        tick = null;
                    }

                    final PoseResult poseForRendering = lastPoseResult;

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        mainView.updateFrame(fxImage, tick, config.getPoseConfidenceThreshold());
                        if (tick != null) {
                            mainView.updateTick(tick);
                        }
                    });

                    frameCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.info("Processing thread interrupted.");
                    break;
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unexpected error in processing loop", e);
                }
            }
            LOG.info("Processing thread exiting.");
        }, "workout-processing");
        processingThread.setDaemon(true);
        processingThread.start();
    }

    // ---- Shutdown ----

    private void shutdownAndExit(Stage stage) {
        LOG.info("Shutting down...");
        processing.set(false);

        if (processingThread != null) {
            processingThread.interrupt();
            try {
                processingThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (cameraManager != null) {
            cameraManager.close();
        }
        if (poseEstimator != null) {
            poseEstimator.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }

        stage.close();
        Platform.exit();
    }

    @Override
    public void stop() {
        processing.set(false);
    }

    // ---- Helpers ----

    private String resolveModelPath(String configPath) {
        // Try the configured path directly
        if (configPath != null) {
            Path direct = Paths.get(configPath);
            if (Files.isRegularFile(direct)) {
                return direct.toAbsolutePath().toString();
            }
        }

        // Try src/main/resources/models/
        String[] candidates = {
                "src/main/resources/models/movenet_singlepose_lightning.onnx",
                "models/movenet_singlepose_lightning.onnx",
                "movenet_singlepose_lightning.onnx",
                "src/main/resources/" + configPath
        };
        for (String candidate : candidates) {
            Path p = Paths.get(candidate);
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath().toString();
            }
        }
        return null;
    }

    private String resolveDbPath(String configDbPath) {
        if (configDbPath == null || configDbPath.isBlank()) {
            return Paths.get(System.getProperty("user.home", "."), ".ai-workout-tracker", "workout_history.db").toString();
        }
        if (Paths.get(configDbPath).isAbsolute()) {
            return configDbPath;
        }
        return Paths.get(System.getProperty("user.home", "."), ".ai-workout-tracker", configDbPath).toString();
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
