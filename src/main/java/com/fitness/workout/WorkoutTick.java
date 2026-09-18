package com.fitness.workout;

import com.fitness.analysis.MovementSample;
import com.fitness.analysis.MovementState;
import com.fitness.pose.PoseResult;

/**
 * Everything the dashboard needs to render one frame, bundled together so
 * the analysis thread can hand a single immutable object to the UI thread
 * via {@code Platform.runLater} instead of exposing its component objects
 * (which are not thread-safe) directly.
 */
public final class WorkoutTick {

    private final PoseResult pose;
    private final MovementSample movementSample;
    private final MovementState movementState;
    private final String feedback;
    private final int repCount;
    private final int currentSetNumber;
    private final int repsInCurrentSet;
    private final long totalWorkoutDurationMs;
    private final long activeDurationMs;
    private final long currentSetDurationMs;
    private final long currentRestDurationMs;
    private final long totalRestDurationMs;
    private final long longestRestMs;
    private final int restPeriodCount;
    private final WorkoutState workoutState;

    public WorkoutTick(PoseResult pose, MovementSample movementSample, MovementState movementState, String feedback,
                        int repCount, int currentSetNumber, int repsInCurrentSet, long totalWorkoutDurationMs,
                        long activeDurationMs, long currentSetDurationMs, long currentRestDurationMs,
                        long totalRestDurationMs, long longestRestMs, int restPeriodCount, WorkoutState workoutState) {
        this.pose = pose;
        this.movementSample = movementSample;
        this.movementState = movementState;
        this.feedback = feedback;
        this.repCount = repCount;
        this.currentSetNumber = currentSetNumber;
        this.repsInCurrentSet = repsInCurrentSet;
        this.totalWorkoutDurationMs = totalWorkoutDurationMs;
        this.activeDurationMs = activeDurationMs;
        this.currentSetDurationMs = currentSetDurationMs;
        this.currentRestDurationMs = currentRestDurationMs;
        this.totalRestDurationMs = totalRestDurationMs;
        this.longestRestMs = longestRestMs;
        this.restPeriodCount = restPeriodCount;
        this.workoutState = workoutState;
    }

    public PoseResult getPose() { return pose; }
    public MovementSample getMovementSample() { return movementSample; }
    public MovementState getMovementState() { return movementState; }
    public String getFeedback() { return feedback; }
    public int getRepCount() { return repCount; }
    public int getCurrentSetNumber() { return currentSetNumber; }
    public int getRepsInCurrentSet() { return repsInCurrentSet; }
    public long getTotalWorkoutDurationMs() { return totalWorkoutDurationMs; }
    public long getActiveDurationMs() { return activeDurationMs; }
    public long getCurrentSetDurationMs() { return currentSetDurationMs; }
    public long getCurrentRestDurationMs() { return currentRestDurationMs; }
    public long getTotalRestDurationMs() { return totalRestDurationMs; }
    public long getLongestRestMs() { return longestRestMs; }
    public int getRestPeriodCount() { return restPeriodCount; }
    public WorkoutState getWorkoutState() { return workoutState; }
}
