package com.fitness.analysis;

import com.fitness.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RestDetector}.
 */
public class RestDetectorTest {

    private RestDetector restDetector;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        config = new AppConfig();
        restDetector = new RestDetector(config);
    }

    @Test
    void testInitialState() {
        assertFalse(restDetector.isResting(), "Should not be resting initially");
        assertEquals(0, restDetector.getTotalRestDurationMs(), "Initial total rest should be 0");
        assertEquals(0, restDetector.getRestPeriodCount(), "Initial rest count should be 0");
    }

    @Test
    void testSustainedInactivityTriggersResting() {
        long timestamp = 1_000_000_000L;
        // Default restEntryThresholdMs = 3000ms, so we need > 3 seconds of inactivity
        // Feed ~4 seconds of near-zero signal
        for (int i = 0; i < 80; i++) {
            MovementSample sample = new MovementSample(timestamp, 0.01, 0, 0, 0.25, true);
            restDetector.update(sample);
            timestamp += 50_000_000L; // 50ms steps = 4000ms total
        }
        assertTrue(restDetector.isResting(), "Should be resting after > 3s inactivity");
    }

    @Test
    void testActiveMovementResetsRestTimer() {
        long timestamp = 1_000_000_000L;
        // Brief inactivity (< threshold)
        for (int i = 0; i < 10; i++) {
            MovementSample sample = new MovementSample(timestamp, 0.01, 0, 0, 0.25, true);
            restDetector.update(sample);
            timestamp += 100_000_000L; // 1 second total
        }

        // Then active movement
        MovementSample active = new MovementSample(timestamp, 5.0, 2.5, 2.5, 0.25, true);
        restDetector.update(active);

        assertFalse(restDetector.isResting(), "Active movement should prevent resting state");
    }

    @Test
    void testRestDurationAccumulates() {
        long timestamp = 1_000_000_000L;
        // 10 seconds of inactivity (200 steps × 50ms)
        for (int i = 0; i < 200; i++) {
            MovementSample sample = new MovementSample(timestamp, 0.0, 0, 0, 0.25, true);
            restDetector.update(sample);
            timestamp += 50_000_000L;
        }
        assertTrue(restDetector.getTotalRestDurationMs() > 0, "Rest duration should accumulate");
        assertTrue(restDetector.getRestPeriodCount() > 0, "Rest period count should increment");
        assertTrue(restDetector.getLongestRestDurationMs() > 0, "Longest rest should be tracked");
    }

    @Test
    void testReset() {
        long timestamp = 1_000_000_000L;
        // Build up some rest
        for (int i = 0; i < 200; i++) {
            MovementSample sample = new MovementSample(timestamp, 0.0, 0, 0, 0.25, true);
            restDetector.update(sample);
            timestamp += 50_000_000L;
        }

        restDetector.reset();

        assertFalse(restDetector.isResting(), "Not resting after reset");
        assertEquals(0, restDetector.getTotalRestDurationMs(), "Total rest 0 after reset");
        assertEquals(0, restDetector.getRestPeriodCount(), "Rest count 0 after reset");
        assertEquals(0, restDetector.getLongestRestDurationMs(), "Longest rest 0 after reset");
    }
}
