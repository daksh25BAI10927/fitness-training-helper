package com.fitness.workout;

import com.fitness.workout.WorkoutState; // imported as per instructions, although in same package

/**
 * Manages all time-based statistics during a workout session.
 */
public class TimerManager {
    private WorkoutState state = WorkoutState.NOT_STARTED;
    
    private long totalWorkoutDurationMs = 0;
    private long activeMovementDurationMs = 0;
    private long currentSetDurationMs = 0;
    
    private long lastTickNanos = 0;
    private int lastSetNumber = -1;

    /**
     * Begins a workout session.
     * @param nowNanos The current timestamp in nanoseconds.
     */
    public void start(long nowNanos) {
        state = WorkoutState.ACTIVE;
        lastTickNanos = nowNanos;
        totalWorkoutDurationMs = 0;
        activeMovementDurationMs = 0;
        currentSetDurationMs = 0;
        lastSetNumber = -1;
    }

    /**
     * Pauses the timer tracking.
     */
    public void pause() {
        if (state == WorkoutState.ACTIVE) {
            state = WorkoutState.PAUSED;
        }
    }

    /**
     * Resumes the timer tracking from pause.
     * @param nowNanos The current timestamp in nanoseconds.
     */
    public void resume(long nowNanos) {
        if (state == WorkoutState.PAUSED) {
            state = WorkoutState.ACTIVE;
            lastTickNanos = nowNanos; // reset tick to ignore paused duration
        }
    }

    /**
     * Stops the workout.
     */
    public void stop() {
        state = WorkoutState.STOPPED;
    }

    /**
     * Resets the manager to the not started state.
     */
    public void reset() {
        state = WorkoutState.NOT_STARTED;
        totalWorkoutDurationMs = 0;
        activeMovementDurationMs = 0;
        currentSetDurationMs = 0;
        lastTickNanos = 0;
        lastSetNumber = -1;
    }

    /**
     * @return The current state of the workout.
     */
    public WorkoutState getState() {
        return state;
    }

    /**
     * Called periodically (e.g., each frame) to update time statistics.
     * @param timestampNanos Current frame timestamp in nanoseconds.
     * @param isResting Whether the user is currently resting.
     * @param currentSetNumber The active set number.
     */
    public void tick(long timestampNanos, boolean isResting, int currentSetNumber) {
        if (state != WorkoutState.ACTIVE) {
            if (state == WorkoutState.PAUSED) {
                lastTickNanos = timestampNanos;
            }
            return;
        }
        
        if (lastTickNanos > 0 && timestampNanos > lastTickNanos) {
            long deltaMs = (timestampNanos - lastTickNanos) / 1_000_000;
            totalWorkoutDurationMs += deltaMs;
            
            if (currentSetNumber != lastSetNumber) {
                currentSetDurationMs = 0;
                lastSetNumber = currentSetNumber;
            }

            if (!isResting) {
                activeMovementDurationMs += deltaMs;
                currentSetDurationMs += deltaMs;
            }
        }
        
        lastTickNanos = timestampNanos;
    }
    
    /**
     * @return Total wall-clock workout time since start (excluding paused periods).
     */
    public long getTotalWorkoutDurationMs() {
        return totalWorkoutDurationMs;
    }
    
    /**
     * @return Total time spent not resting.
     */
    public long getActiveMovementDurationMs() {
        return activeMovementDurationMs;
    }
    
    /**
     * @return Duration of the current set.
     */
    public long getCurrentSetDurationMs() {
        return currentSetDurationMs;
    }
}
