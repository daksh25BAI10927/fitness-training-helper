package com.fitness.analysis;

import com.fitness.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SetDetector.
 */
public class SetDetectorTest {

    private SetDetector setDetector;
    private AppConfig config;
    private RepCounter repCounter;
    private RestDetector restDetector;

    @BeforeEach
    public void setUp() {
        config = new AppConfig();
        setDetector = new SetDetector(config);
        repCounter = new RepCounter(config);
        restDetector = new RestDetector(config);
    }

    @Test
    public void testInitialState() {
        assertEquals(1, setDetector.getCurrentSetNumber(), "Initial set number should be 1");
        assertTrue(setDetector.getCompletedSets().isEmpty(), "No completed sets initially");
    }

    @Test
    public void testSetClosesAfterRest() {
        long timestamp = 0;
        
        // Simulate reps
        for (int i = 0; i <= 200; i++) {
            double val = 2.0 * Math.sin(i * Math.PI / 20.0);
            MovementSample sample = new MovementSample(timestamp, val, val, val, 1.0, true);
            repCounter.update(sample);
            restDetector.update(sample);
            setDetector.update(sample, repCounter, restDetector);
            timestamp += 50_000_000L;
        }
        
        assertTrue(repCounter.getRepCount() >= config.getMinRepsPerSet(), "Need minimum reps to form a set");
        
        // Simulate rest
        for (int i = 0; i < 1000; i++) { // Long rest to exceed setBreakThresholdMs
            MovementSample sample = new MovementSample(timestamp, 0.0, 0, 0, 1.0, true);
            restDetector.update(sample);
            setDetector.update(sample, repCounter, restDetector);
            timestamp += 100_000_000L;
        }
        
        assertEquals(2, setDetector.getCurrentSetNumber(), "Set number should increment after set closes");
        assertEquals(1, setDetector.getCompletedSets().size(), "Should have 1 completed set");
    }

    @Test
    public void testMinRepsPerSetEnforced() {
        long timestamp = 0;
        
        // Insufficient reps
        MovementSample sample = new MovementSample(timestamp, 5.0, 5.0, 5.0, 1.0, true);
        repCounter.update(sample);
        restDetector.update(sample);
        setDetector.update(sample, repCounter, restDetector);
        
        // Simulate rest
        for (int i = 0; i < 500; i++) { 
            sample = new MovementSample(timestamp, 0.0, 0, 0, 1.0, true);
            restDetector.update(sample);
            setDetector.update(sample, repCounter, restDetector);
            timestamp += 100_000_000L;
        }
        
        assertEquals(1, setDetector.getCurrentSetNumber(), "Set should not close due to insufficient reps");
        assertTrue(setDetector.getCompletedSets().isEmpty(), "No sets should be completed");
    }

    @Test
    public void testReset() {
        setDetector.reset();
        assertEquals(1, setDetector.getCurrentSetNumber(), "Set number resets to 1");
        assertTrue(setDetector.getCompletedSets().isEmpty(), "Completed sets cleared");
    }
}
