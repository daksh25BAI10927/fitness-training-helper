package com.fitness.analysis;

/**
 * Coarse movement status, derived each frame from {@link RepCounter} and
 * {@link RestDetector}. This is what the dashboard's STATUS indicator shows.
 */
public enum MovementState {
    /** No person / pose reliably detected yet, or tracking was just lost. */
    NO_POSE_DETECTED,
    /** A person is detected but is not currently moving enough to register as activity. */
    IDLE,
    /** Active repetitive movement is in progress. */
    MOVING,
    /** Sustained inactivity has crossed the rest-entry threshold. */
    RESTING
}
