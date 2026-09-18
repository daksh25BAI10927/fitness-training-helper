package com.fitness.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OneEuroFilter}.
 *
 * <p>The actual OneEuroFilter constructor is {@code OneEuroFilter(double minCutoff, double beta, double dCutoff)}
 * and filter method is {@code filter(double x, long nowNanos)}.</p>
 */
public class OneEuroFilterTest {

    private OneEuroFilter filter;

    @BeforeEach
    void setUp() {
        // minCutoff=1.0, beta=0.0 (no speed adaptation), dCutoff=1.0
        filter = new OneEuroFilter(1.0, 0.0, 1.0);
    }

    @Test
    void testFirstValuePassedThrough() {
        double out = filter.filter(10.0, 1_000_000_000L);
        assertEquals(10.0, out, 0.001, "First value should be returned as-is");
    }

    @Test
    void testConstantInput() {
        long t = 1_000_000_000L;
        filter.filter(10.0, t);
        t += 33_000_000L; // ~30fps
        double out = filter.filter(10.0, t);
        assertEquals(10.0, out, 0.001, "Constant input should produce constant output");
    }

    @Test
    void testNoisyInputSmoothed() {
        long t = 1_000_000_000L;
        filter.filter(10.0, t);

        // Feed slightly noisy values around 10.0
        double maxDeviation = 0;
        for (int i = 0; i < 50; i++) {
            t += 33_000_000L;
            double noisy = 10.0 + (Math.random() - 0.5) * 0.2;
            double out = filter.filter(noisy, t);
            maxDeviation = Math.max(maxDeviation, Math.abs(out - 10.0));
        }
        assertTrue(maxDeviation < 0.15, "Smoothed output should stay close to 10.0, maxDev=" + maxDeviation);
    }

    @Test
    void testStepChangeResponse() {
        long t = 1_000_000_000L;
        filter.filter(10.0, t);

        t += 33_000_000L;
        double out = filter.filter(20.0, t);
        // Should move toward 20 but not reach it instantly (low-pass behavior)
        assertTrue(out > 10.0 && out <= 20.0, "Should move toward step target, got " + out);
    }

    @Test
    void testResetClearsState() {
        long t = 1_000_000_000L;
        filter.filter(10.0, t);
        t += 33_000_000L;
        filter.filter(15.0, t);

        filter.reset();

        t += 33_000_000L;
        double out = filter.filter(20.0, t);
        assertEquals(20.0, out, 0.001, "After reset, first value should be returned as-is");
    }

    @Test
    void testBetaResponsiveness() {
        // High beta = more responsive to fast movement
        OneEuroFilter responsive = new OneEuroFilter(1.0, 1.0, 1.0);
        OneEuroFilter sluggish = new OneEuroFilter(1.0, 0.0, 1.0);

        long t = 1_000_000_000L;
        responsive.filter(0.0, t);
        sluggish.filter(0.0, t);

        t += 33_000_000L;
        double rOut = responsive.filter(10.0, t);
        double sOut = sluggish.filter(10.0, t);

        // Both should move toward 10, but responsive should get closer
        assertTrue(rOut >= sOut, "High beta filter should respond faster, rOut=" + rOut + " sOut=" + sOut);
    }
}
