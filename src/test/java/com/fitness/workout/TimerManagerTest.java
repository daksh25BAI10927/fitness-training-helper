package com.fitness.workout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TimerManager}.
 *
 * <p>TimerManager tracks time using nanoTime-based timestamps. The tick() method
 * signature is: {@code tick(long timestampNanos, boolean isResting, int currentSetNumber)}.
 * We simulate time progression by providing increasing nanoTime values.</p>
 */
public class TimerManagerTest {

    private TimerManager tm;

    @BeforeEach
    void setUp() {
        tm = new TimerManager();
    }

    @Test
    void testInitialState() {
        assertEquals(WorkoutState.NOT_STARTED, tm.getState());
        assertEquals(0, tm.getTotalWorkoutDurationMs());
        assertEquals(0, tm.getActiveMovementDurationMs());
        assertEquals(0, tm.getCurrentSetDurationMs());
    }

    @Test
    void testStartSetsActive() {
        tm.start(1_000_000_000L);
        assertEquals(WorkoutState.ACTIVE, tm.getState());
    }

    @Test
    void testTickAccumulatesTime() {
        long t0 = 1_000_000_000L; // 1 second in nanos
        tm.start(t0);

        // Tick 500ms later, not resting, set 1
        tm.tick(t0 + 500_000_000L, false, 1);
        assertEquals(500, tm.getTotalWorkoutDurationMs());
        assertEquals(500, tm.getActiveMovementDurationMs());

        // Tick another 300ms later
        tm.tick(t0 + 800_000_000L, false, 1);
        assertEquals(800, tm.getTotalWorkoutDurationMs());
        assertEquals(800, tm.getActiveMovementDurationMs());
    }

    @Test
    void testRestingExcludesActiveTime() {
        long t0 = 1_000_000_000L;
        tm.start(t0);

        // 200ms active
        tm.tick(t0 + 200_000_000L, false, 1);
        // 300ms resting
        tm.tick(t0 + 500_000_000L, true, 1);

        assertEquals(500, tm.getTotalWorkoutDurationMs());
        assertEquals(200, tm.getActiveMovementDurationMs());
    }

    @Test
    void testPauseStopsAccumulation() {
        long t0 = 1_000_000_000L;
        tm.start(t0);

        tm.tick(t0 + 100_000_000L, false, 1);
        assertEquals(100, tm.getTotalWorkoutDurationMs());

        tm.pause();
        assertEquals(WorkoutState.PAUSED, tm.getState());

        // Ticks during pause should not add time
        tm.tick(t0 + 500_000_000L, false, 1);
        assertEquals(100, tm.getTotalWorkoutDurationMs());
    }

    @Test
    void testResumeAfterPause() {
        long t0 = 1_000_000_000L;
        tm.start(t0);

        tm.tick(t0 + 100_000_000L, false, 1);
        tm.pause();

        // Resume at t0 + 2 seconds
        long resumeTime = t0 + 2_000_000_000L;
        tm.resume(resumeTime);
        assertEquals(WorkoutState.ACTIVE, tm.getState());

        // Tick 200ms after resume
        tm.tick(resumeTime + 200_000_000L, false, 1);
        // Should be 100ms (before pause) + 200ms (after resume) = 300ms total
        assertEquals(300, tm.getTotalWorkoutDurationMs());
    }

    @Test
    void testStopPreventsAccumulation() {
        long t0 = 1_000_000_000L;
        tm.start(t0);
        tm.tick(t0 + 100_000_000L, false, 1);
        tm.stop();
        assertEquals(WorkoutState.STOPPED, tm.getState());

        tm.tick(t0 + 500_000_000L, false, 1);
        assertEquals(100, tm.getTotalWorkoutDurationMs());
    }

    @Test
    void testReset() {
        long t0 = 1_000_000_000L;
        tm.start(t0);
        tm.tick(t0 + 500_000_000L, false, 1);
        assertTrue(tm.getTotalWorkoutDurationMs() > 0);

        tm.reset();
        assertEquals(WorkoutState.NOT_STARTED, tm.getState());
        assertEquals(0, tm.getTotalWorkoutDurationMs());
        assertEquals(0, tm.getActiveMovementDurationMs());
        assertEquals(0, tm.getCurrentSetDurationMs());
    }

    @Test
    void testSetChangeResetsSetDuration() {
        long t0 = 1_000_000_000L;
        tm.start(t0);

        // Set 1 for 300ms
        tm.tick(t0 + 300_000_000L, false, 1);
        assertEquals(300, tm.getCurrentSetDurationMs());

        // Change to set 2 — should reset set duration
        tm.tick(t0 + 500_000_000L, false, 2);
        // After set change: the 200ms delta goes to new set
        assertTrue(tm.getCurrentSetDurationMs() < 300);
    }
}
