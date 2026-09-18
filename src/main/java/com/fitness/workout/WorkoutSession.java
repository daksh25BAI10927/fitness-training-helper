package com.fitness.workout;

import com.fitness.analysis.SetRecord;
import java.util.List;

/**
 * A summary record of a completed workout session.
 */
public class WorkoutSession {
    private Long id;
    private final long startTimeEpochMillis;
    private final int totalReps;
    private final int totalSets;
    private final long workoutDurationMs;
    private final long activeDurationMs;
    private final long totalBreakDurationMs;
    private final int breakCount;
    private final long longestRestMs;
    private final List<SetRecord> sets;

    /**
     * Constructs a WorkoutSession summary.
     */
    public WorkoutSession(Long id, long startTimeEpochMillis, int totalReps, int totalSets, 
            long workoutDurationMs, long activeDurationMs, long totalBreakDurationMs, 
            int breakCount, long longestRestMs, List<SetRecord> sets) {
        this.id = id;
        this.startTimeEpochMillis = startTimeEpochMillis;
        this.totalReps = totalReps;
        this.totalSets = totalSets;
        this.workoutDurationMs = workoutDurationMs;
        this.activeDurationMs = activeDurationMs;
        this.totalBreakDurationMs = totalBreakDurationMs;
        this.breakCount = breakCount;
        this.longestRestMs = longestRestMs;
        this.sets = sets;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStartTimeEpochMillis() {
        return startTimeEpochMillis;
    }

    public int getTotalReps() {
        return totalReps;
    }

    public int getTotalSets() {
        return totalSets;
    }

    public long getWorkoutDurationMs() {
        return workoutDurationMs;
    }

    public long getActiveDurationMs() {
        return activeDurationMs;
    }

    public long getTotalBreakDurationMs() {
        return totalBreakDurationMs;
    }

    public int getBreakCount() {
        return breakCount;
    }

    public long getLongestRestMs() {
        return longestRestMs;
    }
    
    /**
     * @return The average duration of a rep during this session.
     */
    public long getAverageRepDurationMs() {
        int reps = Math.max(totalReps, 1);
        return workoutDurationMs / reps;
    }
    
    public List<SetRecord> getSets() {
        return sets;
    }
}
