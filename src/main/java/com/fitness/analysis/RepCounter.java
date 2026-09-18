package com.fitness.analysis;

import com.fitness.config.AppConfig;

/**
 * Counts repetitions by finding genuine local extrema (peaks and valleys) in
 * the signed composite movement signal from {@link MovementAnalyzer} - a
 * classic "zigzag" / swing filter, the same family of technique used for
 * peak-trough detection in noisy periodic signals generally. Each pair of
 * consecutive confirmed extrema (a peak followed by a valley, or a valley
 * followed by a peak - whichever order the exercise happens to start in)
 * counts as exactly one repetition, matching how a rep is intuitively
 * defined: one full "away and back".
 *
 * <p>A direction reversal is only <em>confirmed</em> as a real extreme - as
 * opposed to being absorbed as noise within the current swing - once the
 * move away from the last confirmed extreme reaches {@code minProminence}
 * (scaled by {@link AppConfig#getMovementSensitivity()}). This is what
 * rejects landmark jitter: small back-and-forth wiggles near a plateau
 * never accumulate enough prominence to register. A completed rep is
 * additionally required to span at least {@link AppConfig#getMinRepDurationMs()}
 * and to be separated from the previous counted rep by at least
 * {@link AppConfig#getRepCooldownMs()}, rejecting single-frame spikes and
 * rapid bounce right at the threshold respectively.</p>
 *
 * <p>Not thread-safe; owned by the single analysis thread.</p>
 */
public final class RepCounter {

    private static final double PROMINENCE_BASE = 0.18;
    /** sensitivity 0.0 -> x1.4 (harder to trigger), 1.0 -> x0.6 (easier to trigger), 0.5 -> x1.0 */
    private static final double SENSITIVITY_MULTIPLIER_AT_ZERO = 1.4;
    private static final double SENSITIVITY_MULTIPLIER_AT_ONE = 0.6;

    private enum Direction { RISING, FALLING }

    private final AppConfig config;

    private boolean initialized = false;
    private Direction direction = Direction.RISING;
    private double runningExtremeValue;
    private long runningExtremeTimeNanos;
    private double lastConfirmedExtremeValue;
    private long lastConfirmedExtremeTimeNanos;
    private double lastSignal;

    private int extremaSinceLastRep = 0;
    private long repCycleStartNanos = -1;
    private long lastRepCompletedNanos = -1;
    private int repCount = 0;

    public RepCounter(AppConfig config) {
        this.config = config;
    }

    /**
     * Processes one frame's movement sample. No-op (state held) if
     * {@code sample.isPoseUsable()} is false - a momentary tracking dropout
     * should never itself count or corrupt a rep.
     */
    public void update(MovementSample sample) {
        if (!sample.isPoseUsable()) {
            return;
        }
        double signal = sample.getCompositeSignal();
        long now = sample.getTimestampNanos();
        lastSignal = signal;

        if (!initialized) {
            initialized = true;
            runningExtremeValue = signal;
            runningExtremeTimeNanos = now;
            lastConfirmedExtremeValue = signal;
            lastConfirmedExtremeTimeNanos = now;
            return;
        }

        double minProminence = minProminence();

        if (direction == Direction.RISING) {
            if (signal >= runningExtremeValue) {
                runningExtremeValue = signal;
                runningExtremeTimeNanos = now;
            } else {
                tryConfirmExtreme(minProminence);
                direction = Direction.FALLING;
                runningExtremeValue = signal;
                runningExtremeTimeNanos = now;
            }
        } else {
            if (signal <= runningExtremeValue) {
                runningExtremeValue = signal;
                runningExtremeTimeNanos = now;
            } else {
                tryConfirmExtreme(minProminence);
                direction = Direction.RISING;
                runningExtremeValue = signal;
                runningExtremeTimeNanos = now;
            }
        }
    }

    private void tryConfirmExtreme(double minProminence) {
        double prominence = Math.abs(runningExtremeValue - lastConfirmedExtremeValue);
        if (prominence < minProminence) {
            return; // absorbed as noise within the current swing - do not confirm, do not advance
        }
        lastConfirmedExtremeValue = runningExtremeValue;
        lastConfirmedExtremeTimeNanos = runningExtremeTimeNanos;

        if (extremaSinceLastRep == 0) {
            repCycleStartNanos = runningExtremeTimeNanos;
        }
        extremaSinceLastRep++;

        if (extremaSinceLastRep >= 2) {
            long cycleDurationMs = (runningExtremeTimeNanos - repCycleStartNanos) / 1_000_000L;
            boolean longEnough = cycleDurationMs >= config.getMinRepDurationMs();
            boolean pastCooldown = lastRepCompletedNanos < 0
                    || (runningExtremeTimeNanos - lastRepCompletedNanos) / 1_000_000L >= config.getRepCooldownMs();
            if (longEnough && pastCooldown) {
                repCount++;
                lastRepCompletedNanos = runningExtremeTimeNanos;
            }
            extremaSinceLastRep = 0;
            repCycleStartNanos = -1;
        }
    }

    private double minProminence() {
        double sensitivity = clamp01(config.getMovementSensitivity());
        double multiplier = SENSITIVITY_MULTIPLIER_AT_ZERO
                + (SENSITIVITY_MULTIPLIER_AT_ONE - SENSITIVITY_MULTIPLIER_AT_ZERO) * sensitivity;
        return PROMINENCE_BASE * multiplier;
    }

    public int getRepCount() {
        return repCount;
    }

    /** True while roughly mid-swing (past the first confirmed extreme of an in-progress rep pair). Used only for UI wording. */
    public boolean isActive() {
        return Math.abs(lastSignal - lastConfirmedExtremeValue) > minProminence() * 0.3;
    }

    /** How long (ms) the current rep cycle has been building, or 0 if not currently mid-cycle. Used for UI feedback wording only. */
    public long getActiveDurationMs(long nowNanos) {
        if (repCycleStartNanos < 0) {
            return 0;
        }
        return (nowNanos - repCycleStartNanos) / 1_000_000L;
    }

    public void reset() {
        initialized = false;
        direction = Direction.RISING;
        extremaSinceLastRep = 0;
        repCycleStartNanos = -1;
        lastRepCompletedNanos = -1;
        repCount = 0;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
