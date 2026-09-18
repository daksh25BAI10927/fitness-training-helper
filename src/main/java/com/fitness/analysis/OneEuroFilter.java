package com.fitness.analysis;

/**
 * The One Euro Filter (Casiez, Roussel &amp; Vogel, 2012): an adaptive
 * low-pass filter that stays smooth during slow movement (killing jitter)
 * while reducing lag during fast movement. Used here to smooth each
 * landmark coordinate frame-to-frame before it reaches movement analysis,
 * which is what keeps sensor noise from being mistaken for a rep.
 *
 * <p>One instance filters exactly one scalar signal (e.g. a single
 * landmark's x coordinate). {@link com.fitness.analysis.LandmarkSmoother}
 * owns one instance per coordinate per keypoint.</p>
 */
public final class OneEuroFilter {

    private final double minCutoff;
    private final double beta;
    private final double dCutoff;

    private boolean initialized = false;
    private double xPrev;
    private double dxPrev;
    private long tPrevNanos;

    /**
     * @param minCutoff minimum cutoff frequency (Hz) - lower gives smoother output at rest, more lag
     * @param beta      speed coefficient - higher makes the filter react faster to quick movement
     * @param dCutoff   cutoff frequency (Hz) used when filtering the derivative itself; 1.0 is a safe default
     */
    public OneEuroFilter(double minCutoff, double beta, double dCutoff) {
        this.minCutoff = minCutoff;
        this.beta = beta;
        this.dCutoff = dCutoff;
    }

    /**
     * Filters the next raw sample.
     *
     * @param x           raw value at this timestamp
     * @param nowNanos    monotonic timestamp (System.nanoTime())
     * @return the smoothed value
     */
    public double filter(double x, long nowNanos) {
        if (!initialized) {
            initialized = true;
            xPrev = x;
            dxPrev = 0.0;
            tPrevNanos = nowNanos;
            return x;
        }

        double dtSeconds = Math.max(1e-6, (nowNanos - tPrevNanos) / 1_000_000_000.0);

        double dx = (x - xPrev) / dtSeconds;
        double dxSmoothed = lowPass(dx, dxPrev, alpha(dtSeconds, dCutoff));

        double cutoff = minCutoff + beta * Math.abs(dxSmoothed);
        double xSmoothed = lowPass(x, xPrev, alpha(dtSeconds, cutoff));

        xPrev = xSmoothed;
        dxPrev = dxSmoothed;
        tPrevNanos = nowNanos;
        return xSmoothed;
    }

    private static double alpha(double dtSeconds, double cutoffHz) {
        double tau = 1.0 / (2 * Math.PI * cutoffHz);
        return 1.0 / (1.0 + tau / dtSeconds);
    }

    private static double lowPass(double value, double prevFiltered, double alpha) {
        return alpha * value + (1 - alpha) * prevFiltered;
    }

    public void reset() {
        initialized = false;
    }
}
