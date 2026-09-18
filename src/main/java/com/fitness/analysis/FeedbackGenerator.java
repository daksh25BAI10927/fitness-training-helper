package com.fitness.analysis;

import com.fitness.pose.PoseResult;

/**
 * Produces the short feedback string shown on the dashboard, derived only
 * from measurable pose data (landmark visibility, confidence, body scale)
 * and the current movement state - never a medical or biomechanical
 * judgement. Framing/tracking problems always take priority over movement
 * commentary, since movement feedback is meaningless if the pose itself is
 * unreliable.
 */
public final class FeedbackGenerator {

    private static final double STEP_BACK_BODY_SCALE = 0.42;
    private static final double MOVE_CLOSER_BODY_SCALE = 0.09;
    private static final double SMALL_MOVEMENT_SIGNAL = 0.03;
    private static final double NEAR_THRESHOLD_SIGNAL = 0.11;
    private static final long JUST_STARTED_MOVING_MS = 300;

    private FeedbackGenerator() {}

    public static String generate(PoseResult pose, MovementSample sample, MovementState state,
                                   RepCounter repCounter, double confidenceThreshold, double minVisibleFraction) {
        if (pose == null || sample == null || state == MovementState.NO_POSE_DETECTED) {
            return "Make sure your full body is visible";
        }
        if (!pose.hasUsableCore(confidenceThreshold)) {
            return "Make sure your full body is visible";
        }
        if (pose.visibleFraction(confidenceThreshold) < minVisibleFraction) {
            return "Body partially outside camera";
        }
        if (sample.getBodyScale() > STEP_BACK_BODY_SCALE) {
            return "Step back so your whole body fits in frame";
        }
        if (sample.getBodyScale() < MOVE_CLOSER_BODY_SCALE) {
            return "Camera positioning recommended - move a little closer";
        }
        if (pose.meanConfidence() < confidenceThreshold + 0.15) {
            return "Pose confidence low - improve lighting if possible";
        }

        switch (state) {
            case MOVING:
                long activeMs = repCounter.getActiveDurationMs(sample.getTimestampNanos());
                return activeMs >= JUST_STARTED_MOVING_MS ? "Good movement" : "Movement detected";
            case RESTING:
                return "Hold a stable position";
            case IDLE:
            default:
                double magnitude = Math.abs(sample.getCompositeSignal());
                if (magnitude > NEAR_THRESHOLD_SIGNAL) {
                    return "Move a little more";
                }
                if (magnitude > SMALL_MOVEMENT_SIGNAL) {
                    return "Movement too small";
                }
                return "Ready - begin when you are";
        }
    }
}
