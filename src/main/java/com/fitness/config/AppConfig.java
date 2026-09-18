package com.fitness.config;

/**
 * Central set of tunable parameters for the application.
 *
 * <p>Every field has a sensible default so the application works immediately
 * after installation without any manual configuration. Values are loaded
 * from / persisted to a properties file by {@link ConfigManager}.</p>
 *
 * <p>Units are documented per-field. Time values are stored in milliseconds
 * unless noted otherwise, to keep {@link com.fitness.workout.TimerManager}
 * and the analysis pipeline consistent.</p>
 */
public class AppConfig {

    // ---- Camera ----
    /** OpenCV camera index to open by default (0 = first system webcam). */
    private int cameraIndex = 0;
    /** Requested capture width in pixels. Actual value depends on the device. */
    private int cameraWidth = 640;
    /** Requested capture height in pixels. Actual value depends on the device. */
    private int cameraHeight = 480;
    /** Requested capture frame rate. Actual value depends on the device. */
    private int cameraFps = 30;
    /** Capacity of the bounded frame queue between the capture and inference threads. */
    private int frameQueueSize = 2;

    // ---- Pose estimation ----
    /** Path to the ONNX pose model, relative to the resources/models directory or absolute. */
    private String modelPath = "models/movenet_singlepose_lightning.onnx";
    /** Square input resolution expected by the pose model (MoveNet Lightning = 192). */
    private int modelInputSize = 192;
    /** Minimum per-keypoint confidence to treat a landmark as "visible". */
    private double poseConfidenceThreshold = 0.30;
    /** Minimum fraction of core landmarks that must be visible to consider the pose usable. */
    private double minVisibleLandmarkFraction = 0.6;
    /** Run inference on every Nth captured frame (frame skipping for CPU budget). */
    private int inferenceEveryNFrames = 1;

    // ---- Smoothing (One Euro Filter) ----
    /** Minimum cutoff frequency (Hz). Lower = smoother but more lag. */
    private double smoothingMinCutoff = 1.2;
    /** Speed coefficient. Higher = more responsive during fast movement. */
    private double smoothingBeta = 0.02;
    /** Derivative cutoff frequency (Hz), rarely needs tuning. */
    private double smoothingDerivativeCutoff = 1.0;

    // ---- Movement / rep analysis ----
    /**
     * Overall movement sensitivity, 0.0-1.0. Scales the amplitude thresholds
     * used by the rep-cycle detector. Higher = counts smaller movements as reps.
     */
    private double movementSensitivity = 0.5;
    /** Minimum duration (ms) a movement must stay "elevated" to be counted as a rep. */
    private long minRepDurationMs = 250;
    /** Cooldown (ms) after a counted rep before another rep can start, to reject bounce/jitter. */
    private long repCooldownMs = 200;
    /** Minimum reps required before a run of movement can be closed as a "set". */
    private int minRepsPerSet = 1;

    // ---- Rest / break detection ----
    /** Sustained inactivity (ms) before the system reports "resting" state. */
    private long restEntryThresholdMs = 3000;
    /** Sustained inactivity (ms) after which a set is considered finished and a new one begins on resumption. */
    private long setBreakThresholdMs = 8000;

    // ---- Database ----
    /** SQLite database file path, relative to the user's app-data directory unless absolute. */
    private String databasePath = "workout_history.db";

    // ---- UI ----
    /** Target UI refresh rate for the camera preview (frames per second). */
    private int uiRefreshFps = 30;
    /** Dark or light theme identifier for the stylesheet. */
    private String uiTheme = "dark";

    // ---- Getters / setters ----

    public int getCameraIndex() { return cameraIndex; }
    public void setCameraIndex(int cameraIndex) { this.cameraIndex = cameraIndex; }

    public int getCameraWidth() { return cameraWidth; }
    public void setCameraWidth(int cameraWidth) { this.cameraWidth = cameraWidth; }

    public int getCameraHeight() { return cameraHeight; }
    public void setCameraHeight(int cameraHeight) { this.cameraHeight = cameraHeight; }

    public int getCameraFps() { return cameraFps; }
    public void setCameraFps(int cameraFps) { this.cameraFps = cameraFps; }

    public int getFrameQueueSize() { return frameQueueSize; }
    public void setFrameQueueSize(int frameQueueSize) { this.frameQueueSize = frameQueueSize; }

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public int getModelInputSize() { return modelInputSize; }
    public void setModelInputSize(int modelInputSize) { this.modelInputSize = modelInputSize; }

    public double getPoseConfidenceThreshold() { return poseConfidenceThreshold; }
    public void setPoseConfidenceThreshold(double v) { this.poseConfidenceThreshold = v; }

    public double getMinVisibleLandmarkFraction() { return minVisibleLandmarkFraction; }
    public void setMinVisibleLandmarkFraction(double v) { this.minVisibleLandmarkFraction = v; }

    public int getInferenceEveryNFrames() { return inferenceEveryNFrames; }
    public void setInferenceEveryNFrames(int v) { this.inferenceEveryNFrames = v; }

    public double getSmoothingMinCutoff() { return smoothingMinCutoff; }
    public void setSmoothingMinCutoff(double v) { this.smoothingMinCutoff = v; }

    public double getSmoothingBeta() { return smoothingBeta; }
    public void setSmoothingBeta(double v) { this.smoothingBeta = v; }

    public double getSmoothingDerivativeCutoff() { return smoothingDerivativeCutoff; }
    public void setSmoothingDerivativeCutoff(double v) { this.smoothingDerivativeCutoff = v; }

    public double getMovementSensitivity() { return movementSensitivity; }
    public void setMovementSensitivity(double v) { this.movementSensitivity = v; }

    public long getMinRepDurationMs() { return minRepDurationMs; }
    public void setMinRepDurationMs(long v) { this.minRepDurationMs = v; }

    public long getRepCooldownMs() { return repCooldownMs; }
    public void setRepCooldownMs(long v) { this.repCooldownMs = v; }

    public int getMinRepsPerSet() { return minRepsPerSet; }
    public void setMinRepsPerSet(int v) { this.minRepsPerSet = v; }

    public long getRestEntryThresholdMs() { return restEntryThresholdMs; }
    public void setRestEntryThresholdMs(long v) { this.restEntryThresholdMs = v; }

    public long getSetBreakThresholdMs() { return setBreakThresholdMs; }
    public void setSetBreakThresholdMs(long v) { this.setBreakThresholdMs = v; }

    public String getDatabasePath() { return databasePath; }
    public void setDatabasePath(String databasePath) { this.databasePath = databasePath; }

    public int getUiRefreshFps() { return uiRefreshFps; }
    public void setUiRefreshFps(int uiRefreshFps) { this.uiRefreshFps = uiRefreshFps; }

    public String getUiTheme() { return uiTheme; }
    public void setUiTheme(String uiTheme) { this.uiTheme = uiTheme; }
}
