package com.fitness.analysis;

import com.fitness.config.AppConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Infers set boundaries purely from the rep count and rest state: once
 * inactivity has crossed the (longer) set-break threshold - see
 * {@link AppConfig#getSetBreakThresholdMs()}, distinct from the shorter
 * {@link AppConfig#getRestEntryThresholdMs()} used just to display "resting"
 * - and at least {@link AppConfig#getMinRepsPerSet()} reps have happened
 * since the last boundary, the current set is closed. The next completed
 * rep after that opens a new set. A short pause between reps within a set
 * does not close it, because it will not have crossed the (longer)
 * set-break threshold.
 */
public final class SetDetector {

    private final AppConfig config;
    private final List<SetRecord> completedSets = new ArrayList<>();

    private long currentSetStartNanos = -1;
    private int repsAtSetStart = 0;
    private boolean setClosed = false;

    public SetDetector(AppConfig config) {
        this.config = config;
    }

    public void update(MovementSample sample, RepCounter repCounter, RestDetector restDetector) {
        long now = sample.getTimestampNanos();
        int totalReps = repCounter.getRepCount();

        if (currentSetStartNanos < 0) {
            currentSetStartNanos = now;
            repsAtSetStart = totalReps;
            return;
        }

        int repsInCurrentSet = totalReps - repsAtSetStart;

        if (setClosed) {
            if (repsInCurrentSet > 0) {
                setClosed = false;
                currentSetStartNanos = now;
            }
            return;
        }

        if (restDetector.hasCrossedSetBreakThreshold() && repsInCurrentSet >= config.getMinRepsPerSet()) {
            long durationMs = (now - currentSetStartNanos) / 1_000_000L;
            completedSets.add(new SetRecord(completedSets.size() + 1, repsInCurrentSet, durationMs));
            repsAtSetStart = totalReps;
            setClosed = true;
        }
    }

    /** 1-indexed number of the set currently in progress (or about to start). */
    public int getCurrentSetNumber() {
        return completedSets.size() + 1;
    }

    /** Reps completed so far in the set currently in progress (0 right after a set closes). */
    public int getRepsInCurrentSet(RepCounter repCounter) {
        return repCounter.getRepCount() - repsAtSetStart;
    }

    public List<SetRecord> getCompletedSets() {
        return Collections.unmodifiableList(completedSets);
    }

    public void reset() {
        completedSets.clear();
        currentSetStartNanos = -1;
        repsAtSetStart = 0;
        setClosed = false;
    }
}
