package com.fitness.database;

/** A single saved workout session row, as read back from the database for the History screen. */
public final class SessionRecord {

    private final long id;
    private final long startTimeEpochMillis;
    private final int totalReps;
    private final int totalSets;
    private final long workoutDurationMs;
    private final long activeDurationMs;
    private final long totalBreakDurationMs;
    private final int breakCount;
    private final long longestRestMs;
    private final long averageRepDurationMs;

    public SessionRecord(long id, long startTimeEpochMillis, int totalReps, int totalSets, long workoutDurationMs,
                          long activeDurationMs, long totalBreakDurationMs, int breakCount, long longestRestMs,
                          long averageRepDurationMs) {
        this.id = id;
        this.startTimeEpochMillis = startTimeEpochMillis;
        this.totalReps = totalReps;
        this.totalSets = totalSets;
        this.workoutDurationMs = workoutDurationMs;
        this.activeDurationMs = activeDurationMs;
        this.totalBreakDurationMs = totalBreakDurationMs;
        this.breakCount = breakCount;
        this.longestRestMs = longestRestMs;
        this.averageRepDurationMs = averageRepDurationMs;
    }

    public long getId() { return id; }
    public long getStartTimeEpochMillis() { return startTimeEpochMillis; }
    public int getTotalReps() { return totalReps; }
    public int getTotalSets() { return totalSets; }
    public long getWorkoutDurationMs() { return workoutDurationMs; }
    public long getActiveDurationMs() { return activeDurationMs; }
    public long getTotalBreakDurationMs() { return totalBreakDurationMs; }
    public int getBreakCount() { return breakCount; }
    public long getLongestRestMs() { return longestRestMs; }
    public long getAverageRepDurationMs() { return averageRepDurationMs; }
}
