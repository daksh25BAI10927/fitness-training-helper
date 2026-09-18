package com.fitness.analysis;

import com.fitness.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RepCounter.
 */
public class RepCounterTest {

    private RepCounter repCounter;
    private AppConfig config;

    @BeforeEach
    public void setUp() {
        config = new AppConfig();
        repCounter = new RepCounter(config);
    }

    @Test
    public void testInitialState() {
        assertEquals(0, repCounter.getRepCount(), "Initial rep count should be 0");
    }

    @Test
    public void testCleanSinusoidalSignal() {
        long timestamp = 0;
        // Provide a clear signal that oscillates enough to trigger rep detection
        for (int i = 0; i <= 200; i++) {
            double val = 2.0 * Math.sin(i * Math.PI / 20.0);
            MovementSample sample = new MovementSample(timestamp, val, val, val, 1.0, true);
            repCounter.update(sample);
            timestamp += 50_000_000L; // 50ms steps
        }
        assertTrue(repCounter.getRepCount() > 0, "Should count reps for clean sinusoidal signal");
    }

    @Test
    public void testNoiseSignal() {
        long timestamp = 0;
        for (int i = 0; i < 200; i++) {
            double val = (Math.random() - 0.5) * 0.05; // tiny random fluctuations
            MovementSample sample = new MovementSample(timestamp, val, 0, 0, 1.0, true);
            repCounter.update(sample);
            timestamp += 50_000_000L;
        }
        assertEquals(0, repCounter.getRepCount(), "Should not count reps for noise");
    }

    @Test
    public void testInsufficientProminence() {
        long timestamp = 0;
        for (int i = 0; i <= 200; i++) {
            double val = 0.1 * Math.sin(i * Math.PI / 20.0); // very small amplitude
            MovementSample sample = new MovementSample(timestamp, val, 0, 0, 1.0, true);
            repCounter.update(sample);
            timestamp += 50_000_000L;
        }
        assertEquals(0, repCounter.getRepCount(), "Should reject signal with insufficient prominence");
    }

    @Test
    public void testPoseUnusable() {
        long timestamp = 0;
        for (int i = 0; i <= 200; i++) {
            double val = 2.0 * Math.sin(i * Math.PI / 20.0);
            // Pose usable = false
            MovementSample sample = new MovementSample(timestamp, val, val, val, 1.0, false);
            repCounter.update(sample);
            timestamp += 50_000_000L;
        }
        assertEquals(0, repCounter.getRepCount(), "Should ignore unusable poses");
    }

    @Test
    public void testReset() {
        long timestamp = 0;
        for (int i = 0; i <= 200; i++) {
            double val = 2.0 * Math.sin(i * Math.PI / 20.0);
            MovementSample sample = new MovementSample(timestamp, val, val, val, 1.0, true);
            repCounter.update(sample);
            timestamp += 50_000_000L;
        }
        
        repCounter.reset();
        assertEquals(0, repCounter.getRepCount(), "Reset should clear rep count");
    }
}
