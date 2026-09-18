package com.fitness.analysis;

/**
 * The per-frame output of {@link MovementAnalyzer}.
 *
 * <p>{@code compositeSignal} is deliberately <b>signed</b>, not a rectified
 * magnitude. An earlier version of this class computed an always-positive
 * "distance from a slow baseline" - which turned out to be a real bug: for
 * a single-direction repetitive movement (e.g. a squat, which starts and
 * ends at the same "standing" position and moves away and back exactly
 * once), the rectified distance from the *average* position touches its
 * maximum at both the standing extreme and the squat-bottom extreme, and
 * dips to zero twice per rep (at the two midpoints) - i.e. it produces two
 * humps per true repetition, silently double-counting every rep. Keeping
 * the signal signed and detecting genuine local extrema (peaks and valleys)
 * on it, as {@link RepCounter} does, avoids that failure mode entirely; see
 * that class's Javadoc and the project README's "Known Limitations"
 * section for the full explanation.</p>
 */
public final class MovementSample {

    private final long timestampNanos;
    private final double compositeSignal;
    private final double legExtensionDeviation;
    private final double armExtensionDeviation;
    private final double bodyScale;
    private final boolean poseUsable;

    public MovementSample(long timestampNanos, double compositeSignal, double legExtensionDeviation,
                           double armExtensionDeviation, double bodyScale, boolean poseUsable) {
        this.timestampNanos = timestampNanos;
        this.compositeSignal = compositeSignal;
        this.legExtensionDeviation = legExtensionDeviation;
        this.armExtensionDeviation = armExtensionDeviation;
        this.bodyScale = bodyScale;
        this.poseUsable = poseUsable;
    }

    public long getTimestampNanos() { return timestampNanos; }

    /** Signed, body-scale-normalized composite movement signal. Can be negative. */
    public double getCompositeSignal() { return compositeSignal; }

    /** Signed deviation of (ankle-to-hip vertical distance) from its own slow baseline - captures squats/lunges. */
    public double getLegExtensionDeviation() { return legExtensionDeviation; }

    /** Signed deviation of (wrist-to-shoulder distance, bilateral average) from its own slow baseline - captures curls/presses/raises. */
    public double getArmExtensionDeviation() { return armExtensionDeviation; }

    public double getBodyScale() { return bodyScale; }
    public boolean isPoseUsable() { return poseUsable; }
}
