package com.fitness.pose;

import java.util.EnumMap;
import java.util.Map;

/**
 * The full result of running pose estimation on a single frame: one
 * {@link Landmark} per {@link KeypointType}, plus the timestamp it was
 * captured at (nanoTime-based, monotonic - safe for duration math, not
 * wall-clock display).
 */
public final class PoseResult {

    private final Map<KeypointType, Landmark> landmarks;
    private final long timestampNanos;

    public PoseResult(Map<KeypointType, Landmark> landmarks, long timestampNanos) {
        this.landmarks = new EnumMap<>(landmarks);
        this.timestampNanos = timestampNanos;
    }

    public Landmark get(KeypointType type) {
        return landmarks.get(type);
    }

    public Map<KeypointType, Landmark> allLandmarks() {
        return landmarks;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    /** Fraction of all 17 keypoints whose score is >= threshold. */
    public double visibleFraction(double confidenceThreshold) {
        long visible = landmarks.values().stream()
                .filter(l -> l.isVisible(confidenceThreshold))
                .count();
        return landmarks.isEmpty() ? 0.0 : (double) visible / KeypointType.COUNT;
    }

    /** Mean confidence score across all keypoints. */
    public double meanConfidence() {
        return landmarks.values().stream().mapToDouble(Landmark::getScore).average().orElse(0.0);
    }

    /**
     * True if the "core" landmarks needed for generic movement analysis
     * (shoulders, hips, wrists, knees, ankles) are visible. Face keypoints
     * are excluded since many exercises face away from or below the camera.
     */
    public boolean hasUsableCore(double confidenceThreshold) {
        KeypointType[] core = {
                KeypointType.LEFT_SHOULDER, KeypointType.RIGHT_SHOULDER,
                KeypointType.LEFT_HIP, KeypointType.RIGHT_HIP
        };
        for (KeypointType t : core) {
            Landmark l = landmarks.get(t);
            if (l == null || !l.isVisible(confidenceThreshold)) {
                return false;
            }
        }
        return true;
    }
}
