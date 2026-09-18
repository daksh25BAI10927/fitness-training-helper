package com.fitness.pose;

/**
 * Thrown when pose estimation cannot proceed: model file missing/invalid,
 * inference runtime failure, or an unsupported input frame.
 */
public class PoseEstimationException extends Exception {

    private static final long serialVersionUID = 1L;

    public PoseEstimationException(String message) {
        super(message);
    }

    public PoseEstimationException(String message, Throwable cause) {
        super(message, cause);
    }
}
