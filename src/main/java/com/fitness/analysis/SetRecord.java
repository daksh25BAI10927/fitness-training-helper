package com.fitness.analysis;

/** An immutable summary of one completed set, produced by {@link SetDetector}. */
public final class SetRecord {

    private final int setNumber;
    private final int repCount;
    private final long durationMs;

    public SetRecord(int setNumber, int repCount, long durationMs) {
        this.setNumber = setNumber;
        this.repCount = repCount;
        this.durationMs = durationMs;
    }

    public int getSetNumber() { return setNumber; }
    public int getRepCount() { return repCount; }
    public long getDurationMs() { return durationMs; }
}
