package com.fitness.analysis;

import com.fitness.config.AppConfig;

/**
 * Tracks how long it has been since any meaningful movement was observed,
 * and from that derives the "resting" status plus the cumulative rest
 * statistics requested by the spec (total rest time, longest single rest,
 * number of rest periods).
 *
 * <p>A frame counts as "active" if the composite movement signal's
 * magnitude is above a small low-activity floor. This relies on
 * {@link MovementAnalyzer}'s adaptive baseline (see that class's Javadoc)
 * settling back to ~0 within roughly a second of genuine stillness -
 * deliberately not on {@link RepCounter#isActive()}, which was tried first
 * but turned out to be a poor "currently resting" signal: it measures
 * distance from the last *confirmed* extreme, which stays stale (and can
 * read as "active" for a long time) whenever the signal has moved on and
 * decayed without a new extreme ever being confirmed.</p>
 *
 * <p>Rest duration only formally starts once inactivity has been sustained
 * for {@link AppConfig#getRestEntryThresholdMs()}, so ordinary short pauses
 * between reps are not misreported as a break - this is the hysteresis /
 * "don't treat every tiny pause as rest" behaviour called for by the spec.
 * Time-based accumulation (via frame timestamps, not frame counts) means
 * the totals stay correct even if the camera's FPS drifts.</p>
 */
public final class RestDetector {

    /** Composite-signal floor below which a frame is considered "not moving", in body-scale units. */
    private static final double LOW_ACTIVITY_THRESHOLD = 0.05;

    private final AppConfig config;

    private long lastActiveNanos = -1;
    private long lastUpdateNanos = -1;
    private long totalRestNanos = 0;
    private long longestRestNanos = 0;
    private int restPeriodCount = 0;
    private boolean wasResting = false;
    private long currentRestNanos = 0;

    public RestDetector(AppConfig config) {
        this.config = config;
    }

    public void update(MovementSample sample) {
        long now = sample.getTimestampNanos();
        if (lastUpdateNanos < 0) {
            lastUpdateNanos = now;
            lastActiveNanos = now;
            return;
        }
        long dtNanos = now - lastUpdateNanos;
        lastUpdateNanos = now;

        boolean instantActive = isSignalActive(sample);
        if (instantActive) {
            lastActiveNanos = now;
        }

        currentRestNanos = now - lastActiveNanos;
        long restEntryNanos = config.getRestEntryThresholdMs() * 1_000_000L;
        boolean restingNow = currentRestNanos >= restEntryNanos;

        if (restingNow) {
            totalRestNanos += dtNanos;
            if (currentRestNanos > longestRestNanos) {
                longestRestNanos = currentRestNanos;
            }
            if (!wasResting) {
                restPeriodCount++;
            }
        }
        wasResting = restingNow;
    }

    public boolean isResting() {
        return wasResting;
    }

    public long getCurrentRestDurationMs() {
        return currentRestNanos / 1_000_000L;
    }

    public long getTotalRestDurationMs() {
        return totalRestNanos / 1_000_000L;
    }

    public long getLongestRestDurationMs() {
        return longestRestNanos / 1_000_000L;
    }

    public int getRestPeriodCount() {
        return restPeriodCount;
    }

    /** True once inactivity has crossed the (longer) set-break threshold - used by {@link SetDetector}. */
    public boolean hasCrossedSetBreakThreshold() {
        return currentRestNanos >= config.getSetBreakThresholdMs() * 1_000_000L;
    }

    public void reset() {
        lastActiveNanos = -1;
        lastUpdateNanos = -1;
        totalRestNanos = 0;
        longestRestNanos = 0;
        restPeriodCount = 0;
        wasResting = false;
        currentRestNanos = 0;
    }

    /** Shared "is anything meaningfully moving right now" check, reused by {@link com.fitness.workout.WorkoutManager} for the live status display. */
    public static boolean isSignalActive(MovementSample sample) {
        return sample.isPoseUsable() && Math.abs(sample.getCompositeSignal()) > LOW_ACTIVITY_THRESHOLD;
    }
}
