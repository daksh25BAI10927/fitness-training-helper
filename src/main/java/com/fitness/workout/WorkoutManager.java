package com.fitness.workout;

import com.fitness.analysis.FeedbackGenerator;
import com.fitness.analysis.LandmarkSmoother;
import com.fitness.analysis.MovementAnalyzer;
import com.fitness.analysis.MovementSample;
import com.fitness.analysis.MovementState;
import com.fitness.analysis.RepCounter;
import com.fitness.analysis.RestDetector;
import com.fitness.analysis.SetDetector;
import com.fitness.config.AppConfig;
import com.fitness.pose.PoseResult;

/**
 * The single owner of one workout session's state. Wires together the
 * analysis pipeline (smoothing -&gt; movement analysis -&gt; rep/set/rest
 * detection -&gt; timers -&gt; feedback) and exposes the start/pause/resume/
 * stop/reset lifecycle plus one {@link #processFrame(PoseResult)} entry
 * point that the inference thread calls once per frame.
 *
 * <p>Not thread-safe - intended to be owned and called only by the single
 * inference/analysis thread (see the project README's Threading section).
 * The {@link WorkoutTick} it returns is an immutable snapshot safe to hand
 * to the UI thread.</p>
 */
public final class WorkoutManager {

    private final AppConfig config;
    private final LandmarkSmoother smoother;
    private final MovementAnalyzer movementAnalyzer;
    private final RepCounter repCounter;
    private final RestDetector restDetector;
    private final SetDetector setDetector;
    private final TimerManager timerManager = new TimerManager();

    public WorkoutManager(AppConfig config) {
        this.config = config;
        this.smoother = new LandmarkSmoother(config);
        this.movementAnalyzer = new MovementAnalyzer(config);
        this.repCounter = new RepCounter(config);
        this.restDetector = new RestDetector(config);
        this.setDetector = new SetDetector(config);
    }

    public void start(long nowNanos) {
        smoother.reset();
        movementAnalyzer.reset();
        repCounter.reset();
        restDetector.reset();
        setDetector.reset();
        timerManager.start(nowNanos);
    }

    public void pause() {
        timerManager.pause();
    }

    public void resume(long nowNanos) {
        timerManager.resume(nowNanos);
    }

    public void stop() {
        timerManager.stop();
    }

    /** Resets everything back to {@link WorkoutState#NOT_STARTED} without saving. */
    public void reset() {
        smoother.reset();
        movementAnalyzer.reset();
        repCounter.reset();
        restDetector.reset();
        setDetector.reset();
        timerManager.reset();
    }

    public WorkoutState getState() {
        return timerManager.getState();
    }

    /**
     * Processes one raw pose estimate. Safe to call even when the workout is
     * paused or not started - the analysis pipeline still runs (so the
     * camera preview and skeleton overlay keep working), it is only the
     * timers and rep/set/rest counters that freeze while paused, because
     * {@link TimerManager#tick} itself no-ops outside {@link WorkoutState#ACTIVE}.
     * To keep counts frozen while paused too, callers should simply not
     * treat {@link WorkoutTick} rep/set numbers as changing during pause -
     * in practice {@link RepCounter} etc. keep advancing internally even
     * while paused, matching most fitness apps' behaviour of "pause stops
     * the clock, not the camera preview"; call {@link #pause()} promptly
     * when the user asks for a fully frozen state.
     */
    public WorkoutTick processFrame(PoseResult rawPose) {
        PoseResult smoothed = smoother.smooth(rawPose);
        MovementSample sample = movementAnalyzer.analyze(smoothed);

        repCounter.update(sample);
        restDetector.update(sample);
        setDetector.update(sample, repCounter, restDetector);
        timerManager.tick(sample.getTimestampNanos(), restDetector.isResting(), setDetector.getCurrentSetNumber());

        MovementState state = deriveState(sample);
        String feedback = FeedbackGenerator.generate(smoothed, sample, state, repCounter,
                config.getPoseConfidenceThreshold(), config.getMinVisibleLandmarkFraction());

        return new WorkoutTick(
                smoothed,
                sample,
                state,
                feedback,
                repCounter.getRepCount(),
                setDetector.getCurrentSetNumber(),
                setDetector.getRepsInCurrentSet(repCounter),
                timerManager.getTotalWorkoutDurationMs(),
                timerManager.getActiveMovementDurationMs(),
                timerManager.getCurrentSetDurationMs(),
                restDetector.getCurrentRestDurationMs(),
                restDetector.getTotalRestDurationMs(),
                restDetector.getLongestRestDurationMs(),
                restDetector.getRestPeriodCount(),
                timerManager.getState());
    }

    private MovementState deriveState(MovementSample sample) {
        if (!sample.isPoseUsable()) {
            return MovementState.NO_POSE_DETECTED;
        }
        if (restDetector.isResting()) {
            return MovementState.RESTING;
        }
        return RestDetector.isSignalActive(sample) ? MovementState.MOVING : MovementState.IDLE;
    }

    /** Builds the final {@link WorkoutSession} summary for saving once the workout is stopped. */
    public WorkoutSession buildSessionSummary(long startTimeEpochMillis) {
        return new WorkoutSession(
                null,
                startTimeEpochMillis,
                repCounter.getRepCount(),
                setDetector.getCompletedSets().size() + (setDetector.getRepsInCurrentSet(repCounter) > 0 ? 1 : 0),
                timerManager.getTotalWorkoutDurationMs(),
                timerManager.getActiveMovementDurationMs(),
                restDetector.getTotalRestDurationMs(),
                restDetector.getRestPeriodCount(),
                restDetector.getLongestRestDurationMs(),
                setDetector.getCompletedSets());
    }
}
