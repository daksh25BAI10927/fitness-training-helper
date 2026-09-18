package com.fitness.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads {@link AppConfig} from a properties file in the user's application-data
 * directory, falling back to built-in defaults whenever the file is missing,
 * unreadable, or contains invalid values. Never throws out of the app on a
 * bad config file - it logs a warning and keeps going with defaults instead,
 * per the "corrupted configuration" failure mode the app must survive.
 */
public final class ConfigManager {

    private static final Logger LOG = Logger.getLogger(ConfigManager.class.getName());
    private static final String APP_DIR_NAME = ".ai-workout-tracker";
    private static final String CONFIG_FILE_NAME = "config.properties";

    private final Path configFile;
    private AppConfig config;

    public ConfigManager() {
        this(defaultConfigDir().resolve(CONFIG_FILE_NAME));
    }

    /** Package-visible constructor for tests, allowing an arbitrary config file location. */
    ConfigManager(Path configFile) {
        this.configFile = configFile;
    }

    private static Path defaultConfigDir() {
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, APP_DIR_NAME);
    }

    /** Loads config from disk, or returns defaults (and writes them out) if not present/invalid. */
    public synchronized AppConfig load() {
        AppConfig cfg = new AppConfig();
        Properties props = new Properties();

        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
                applyProperties(cfg, props);
                LOG.info("Loaded configuration from " + configFile);
            } catch (IOException | NumberFormatException e) {
                LOG.log(Level.WARNING,
                        "Configuration file at " + configFile + " could not be read (" + e.getMessage()
                                + "). Falling back to defaults.", e);
                cfg = new AppConfig();
            }
        } else {
            LOG.info("No configuration file found at " + configFile + "; using defaults.");
        }

        this.config = cfg;
        // Ensure a valid file exists on disk after the first run.
        save(cfg);
        return cfg;
    }

    /** Persists the given config to disk. Failures are logged, never thrown. */
    public synchronized void save(AppConfig cfg) {
        try {
            Files.createDirectories(configFile.getParent());
            Properties props = toProperties(cfg);
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "AI Real-Time Workout & Movement Tracker configuration");
            }
            this.config = cfg;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save configuration to " + configFile, e);
        }
    }

    public AppConfig current() {
        return config != null ? config : load();
    }

    // ---- helpers ----

    private static void applyProperties(AppConfig cfg, Properties p) {
        cfg.setCameraIndex(getInt(p, "camera.index", cfg.getCameraIndex()));
        cfg.setCameraWidth(getInt(p, "camera.width", cfg.getCameraWidth()));
        cfg.setCameraHeight(getInt(p, "camera.height", cfg.getCameraHeight()));
        cfg.setCameraFps(getInt(p, "camera.fps", cfg.getCameraFps()));
        cfg.setFrameQueueSize(getInt(p, "camera.frameQueueSize", cfg.getFrameQueueSize()));

        cfg.setModelPath(p.getProperty("pose.modelPath", cfg.getModelPath()));
        cfg.setModelInputSize(getInt(p, "pose.modelInputSize", cfg.getModelInputSize()));
        cfg.setPoseConfidenceThreshold(getDouble(p, "pose.confidenceThreshold", cfg.getPoseConfidenceThreshold()));
        cfg.setMinVisibleLandmarkFraction(getDouble(p, "pose.minVisibleFraction", cfg.getMinVisibleLandmarkFraction()));
        cfg.setInferenceEveryNFrames(getInt(p, "pose.inferenceEveryNFrames", cfg.getInferenceEveryNFrames()));

        cfg.setSmoothingMinCutoff(getDouble(p, "smoothing.minCutoff", cfg.getSmoothingMinCutoff()));
        cfg.setSmoothingBeta(getDouble(p, "smoothing.beta", cfg.getSmoothingBeta()));
        cfg.setSmoothingDerivativeCutoff(getDouble(p, "smoothing.derivativeCutoff", cfg.getSmoothingDerivativeCutoff()));

        cfg.setMovementSensitivity(getDouble(p, "movement.sensitivity", cfg.getMovementSensitivity()));
        cfg.setMinRepDurationMs(getLong(p, "movement.minRepDurationMs", cfg.getMinRepDurationMs()));
        cfg.setRepCooldownMs(getLong(p, "movement.repCooldownMs", cfg.getRepCooldownMs()));
        cfg.setMinRepsPerSet(getInt(p, "movement.minRepsPerSet", cfg.getMinRepsPerSet()));

        cfg.setRestEntryThresholdMs(getLong(p, "rest.entryThresholdMs", cfg.getRestEntryThresholdMs()));
        cfg.setSetBreakThresholdMs(getLong(p, "rest.setBreakThresholdMs", cfg.getSetBreakThresholdMs()));

        cfg.setDatabasePath(p.getProperty("database.path", cfg.getDatabasePath()));

        cfg.setUiRefreshFps(getInt(p, "ui.refreshFps", cfg.getUiRefreshFps()));
        cfg.setUiTheme(p.getProperty("ui.theme", cfg.getUiTheme()));
    }

    private static Properties toProperties(AppConfig cfg) {
        Properties p = new Properties();
        p.setProperty("camera.index", String.valueOf(cfg.getCameraIndex()));
        p.setProperty("camera.width", String.valueOf(cfg.getCameraWidth()));
        p.setProperty("camera.height", String.valueOf(cfg.getCameraHeight()));
        p.setProperty("camera.fps", String.valueOf(cfg.getCameraFps()));
        p.setProperty("camera.frameQueueSize", String.valueOf(cfg.getFrameQueueSize()));

        p.setProperty("pose.modelPath", cfg.getModelPath());
        p.setProperty("pose.modelInputSize", String.valueOf(cfg.getModelInputSize()));
        p.setProperty("pose.confidenceThreshold", String.valueOf(cfg.getPoseConfidenceThreshold()));
        p.setProperty("pose.minVisibleFraction", String.valueOf(cfg.getMinVisibleLandmarkFraction()));
        p.setProperty("pose.inferenceEveryNFrames", String.valueOf(cfg.getInferenceEveryNFrames()));

        p.setProperty("smoothing.minCutoff", String.valueOf(cfg.getSmoothingMinCutoff()));
        p.setProperty("smoothing.beta", String.valueOf(cfg.getSmoothingBeta()));
        p.setProperty("smoothing.derivativeCutoff", String.valueOf(cfg.getSmoothingDerivativeCutoff()));

        p.setProperty("movement.sensitivity", String.valueOf(cfg.getMovementSensitivity()));
        p.setProperty("movement.minRepDurationMs", String.valueOf(cfg.getMinRepDurationMs()));
        p.setProperty("movement.repCooldownMs", String.valueOf(cfg.getRepCooldownMs()));
        p.setProperty("movement.minRepsPerSet", String.valueOf(cfg.getMinRepsPerSet()));

        p.setProperty("rest.entryThresholdMs", String.valueOf(cfg.getRestEntryThresholdMs()));
        p.setProperty("rest.setBreakThresholdMs", String.valueOf(cfg.getSetBreakThresholdMs()));

        p.setProperty("database.path", cfg.getDatabasePath());

        p.setProperty("ui.refreshFps", String.valueOf(cfg.getUiRefreshFps()));
        p.setProperty("ui.theme", cfg.getUiTheme());
        return p;
    }

    private static int getInt(Properties p, String key, int fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            LOG.warning("Invalid integer for '" + key + "'='" + v + "', using default " + fallback);
            return fallback;
        }
    }

    private static long getLong(Properties p, String key, long fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            LOG.warning("Invalid long for '" + key + "'='" + v + "', using default " + fallback);
            return fallback;
        }
    }

    private static double getDouble(Properties p, String key, double fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            LOG.warning("Invalid double for '" + key + "'='" + v + "', using default " + fallback);
            return fallback;
        }
    }
}
