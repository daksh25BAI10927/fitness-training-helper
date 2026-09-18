package com.fitness.analysis;

import com.fitness.config.AppConfig;
import com.fitness.pose.KeypointType;
import com.fitness.pose.Landmark;
import com.fitness.pose.PoseResult;

/**
 * Converts a stream of (already One-Euro-smoothed) {@link PoseResult}s into
 * a single <b>signed</b> scalar "movement" signal per frame, without any
 * notion of which exercise is being performed.
 *
 * <p><b>Design.</b> Two independent, physically meaningful distance
 * measurements are tracked, each normalized by the person's own body scale
 * (torso length) so the signal is invariant to how close they stand to the
 * camera:</p>
 * <ul>
 *   <li><b>Leg extension</b> - the vertical distance between the ankle
 *       midpoint and the hip midpoint. This shrinks during a squat/lunge
 *       and returns to its standing value on the way back up.</li>
 *   <li><b>Arm extension</b> - the bilateral average of wrist-to-shoulder
 *       distance. This grows when the arms extend away from the torso
 *       (curls, presses, raises) and shrinks back.</li>
 * </ul>
 * <p>Each measurement is compared, <em>with its sign kept</em>, against its
 * own baseline (see {@link AdaptiveBaseline}) to get a signed deviation, and
 * the two signed deviations are simply summed into one composite signal.
 * The two are added specifically because both are built the same way
 * (signed deviation of a physically meaningful, non-negative distance from
 * its own baseline) and are rarely large with opposite sign for the same
 * repetition, so addition combines whichever channel a given exercise
 * actually drives without one masking the other.</p>
 *
 * <p><b>Why signed, not rectified.</b> An earlier version of this class
 * used the unsigned Euclidean distance of the center of mass from a slow
 * position baseline. That signal is mathematically guaranteed to have
 * <em>two</em> humps per repetition whenever the true "rest" position sits
 * at one extreme of the movement (which it almost always does): the
 * baseline settles near the temporal <em>mean</em> of the oscillation, so
 * both the rest position and the movement's peak end up roughly equidistant
 * from it, and the signal dips through zero at the two midpoints of the
 * cycle instead of once. Keeping the signal signed and locating genuine
 * local extrema on it (done by {@link RepCounter}) avoids that failure
 * mode: a clean single-direction oscillation has exactly one peak and one
 * valley per period, regardless of where its mean happens to sit.</p>
 *
 * <p>This is a deliberate, documented simplification, not a claim that it
 * perfectly separates "real" repetitive exercise from any other repetitive
 * body motion - see the Known Limitations section of the project README.</p>
 */
public final class MovementAnalyzer {

    private static final double BODY_SCALE_TAU_SECONDS = 1.0;
    private static final double DEFAULT_BODY_SCALE = 0.25;

    private final AppConfig config;

    private boolean initialized = false;
    private long lastTimestampNanos;
    private double bodyScaleEma = DEFAULT_BODY_SCALE;

    private final AdaptiveBaseline legBaseline = new AdaptiveBaseline();
    private final AdaptiveBaseline armBaseline = new AdaptiveBaseline();

    public MovementAnalyzer(AppConfig config) {
        this.config = config;
    }

    /**
     * Analyzes one smoothed pose. Returns a sample with {@code poseUsable=false}
     * (and a zero signal) if the core torso landmarks aren't confidently
     * visible - callers should treat that as "no reliable movement data this
     * frame" rather than as "the person stopped moving".
     */
    public MovementSample analyze(PoseResult pose) {
        double confidenceThreshold = config.getPoseConfidenceThreshold();
        long now = pose.getTimestampNanos();

        if (!pose.hasUsableCore(confidenceThreshold)) {
            return new MovementSample(now, 0.0, 0.0, 0.0, bodyScaleEma, false);
        }

        Landmark ls = pose.get(KeypointType.LEFT_SHOULDER);
        Landmark rs = pose.get(KeypointType.RIGHT_SHOULDER);
        Landmark lh = pose.get(KeypointType.LEFT_HIP);
        Landmark rh = pose.get(KeypointType.RIGHT_HIP);

        double shoulderMidX = (ls.getX() + rs.getX()) / 2.0;
        double shoulderMidY = (ls.getY() + rs.getY()) / 2.0;
        double hipMidX = (lh.getX() + rh.getX()) / 2.0;
        double hipMidY = (lh.getY() + rh.getY()) / 2.0;

        double torsoLength = Math.sqrt(
                Math.pow(shoulderMidX - hipMidX, 2) + Math.pow(shoulderMidY - hipMidY, 2));

        if (!initialized) {
            initialized = true;
            lastTimestampNanos = now;
            bodyScaleEma = torsoLength > 1e-6 ? torsoLength : DEFAULT_BODY_SCALE;
            return new MovementSample(now, 0.0, 0.0, 0.0, bodyScaleEma, true);
        }

        double dtSeconds = Math.max(1e-6, (now - lastTimestampNanos) / 1_000_000_000.0);
        lastTimestampNanos = now;

        double bodyScaleAlpha = emaAlpha(dtSeconds, BODY_SCALE_TAU_SECONDS);
        if (torsoLength > 1e-6) {
            bodyScaleEma = bodyScaleAlpha * torsoLength + (1 - bodyScaleAlpha) * bodyScaleEma;
        }
        double bodyScale = bodyScaleEma > 1e-6 ? bodyScaleEma : DEFAULT_BODY_SCALE;

        // ---- Leg extension: ankle-to-hip vertical distance ----
        double legDeviation = 0.0;
        Landmark la = pose.get(KeypointType.LEFT_ANKLE);
        Landmark ra = pose.get(KeypointType.RIGHT_ANKLE);
        if (isVisible(la, confidenceThreshold) && isVisible(ra, confidenceThreshold)) {
            double ankleMidY = (la.getY() + ra.getY()) / 2.0;
            double legExtension = Math.abs(ankleMidY - hipMidY) / bodyScale;
            legDeviation = legBaseline.update(legExtension, dtSeconds);
        }

        // ---- Arm extension: bilateral average wrist-to-shoulder distance ----
        double armDeviation = 0.0;
        Landmark lw = pose.get(KeypointType.LEFT_WRIST);
        Landmark rw = pose.get(KeypointType.RIGHT_WRIST);
        Double leftArm = (isVisible(lw, confidenceThreshold) && isVisible(ls, confidenceThreshold))
                ? lw.distanceTo(ls) / bodyScale : null;
        Double rightArm = (isVisible(rw, confidenceThreshold) && isVisible(rs, confidenceThreshold))
                ? rw.distanceTo(rs) / bodyScale : null;
        if (leftArm != null || rightArm != null) {
            double armExtension = (leftArm != null && rightArm != null)
                    ? (leftArm + rightArm) / 2.0
                    : (leftArm != null ? leftArm : rightArm);
            armDeviation = armBaseline.update(armExtension, dtSeconds);
        }

        double composite = legDeviation + armDeviation;
        return new MovementSample(now, composite, legDeviation, armDeviation, bodyScale, true);
    }

    private static boolean isVisible(Landmark l, double confidenceThreshold) {
        return l != null && l.isVisible(confidenceThreshold);
    }

    private static double emaAlpha(double dtSeconds, double tauSeconds) {
        return 1.0 - Math.exp(-dtSeconds / tauSeconds);
    }

    /** Clears all baseline/state tracking, e.g. after tracking was lost for a while or a session reset. */
    public void reset() {
        initialized = false;
        legBaseline.reset();
        armBaseline.reset();
        bodyScaleEma = DEFAULT_BODY_SCALE;
    }

    /**
     * A baseline follower whose own update speed depends on how "quiet" the
     * tracked channel currently is.
     *
     * <p>A plain fixed-time-constant EMA baseline has to choose between two
     * conflicting needs: it must be slow relative to a rep's duration (or it
     * chases the oscillation itself, which was the double-hump bug described
     * on {@link MovementAnalyzer}), yet it must also converge quickly once
     * the person genuinely stops moving (or the stale gap between the last
     * true position and the still-catching-up baseline lingers as a slow
     * decaying "phantom" signal - which was measured, during development, to
     * both delay rest detection by several seconds and occasionally cross
     * the rep-detection threshold on its way down, i.e. count a rep that
     * never happened).</p>
     *
     * <p>The fix is to measure a short-term "activity level" (a fast EMA of
     * the channel's own rate of change) and use it to pick the baseline's
     * time constant each frame: a long, stable time constant
     * ({@link #SLOW_TAU_SECONDS}) while the channel is actively swinging,
     * and a short one ({@link #FAST_TAU_SECONDS}) once activity drops below
     * {@link #QUIET_SPEED_THRESHOLD} - which is exactly the "camera noise /
     * landmark jitter" regime, so quiet periods snap to a new resting
     * baseline in about a second instead of many seconds.</p>
     */
    private static final class AdaptiveBaseline {

        private static final double SLOW_TAU_SECONDS = 6.0;
        private static final double FAST_TAU_SECONDS = 0.75;
        private static final double ACTIVITY_TAU_SECONDS = 0.3;
        private static final double QUIET_SPEED_THRESHOLD = 0.15;

        private boolean valid = false;
        private double baseline;
        private double lastRaw;
        private double activityLevel;

        double update(double raw, double dtSeconds) {
            if (!valid) {
                valid = true;
                baseline = raw;
                lastRaw = raw;
                activityLevel = 0.0;
                return 0.0;
            }

            double instantSpeed = Math.abs(raw - lastRaw) / dtSeconds;
            lastRaw = raw;
            double activityAlpha = emaAlpha(dtSeconds, ACTIVITY_TAU_SECONDS);
            activityLevel = activityAlpha * instantSpeed + (1 - activityAlpha) * activityLevel;

            double tau = activityLevel < QUIET_SPEED_THRESHOLD ? FAST_TAU_SECONDS : SLOW_TAU_SECONDS;
            double baselineAlpha = emaAlpha(dtSeconds, tau);
            baseline = baselineAlpha * raw + (1 - baselineAlpha) * baseline;

            return raw - baseline;
        }

        void reset() {
            valid = false;
            activityLevel = 0.0;
        }
    }
}
