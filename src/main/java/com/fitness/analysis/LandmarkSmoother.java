package com.fitness.analysis;

import com.fitness.config.AppConfig;
import com.fitness.pose.KeypointType;
import com.fitness.pose.Landmark;
import com.fitness.pose.PoseResult;

import java.util.EnumMap;
import java.util.Map;

/**
 * Smooths a stream of {@link PoseResult}s frame-to-frame by running an
 * independent {@link OneEuroFilter} over the x and y coordinate of each of
 * the 17 keypoints. Confidence scores are passed through unfiltered -
 * only position is smoothed.
 *
 * <p>Stateful: one instance must be reused across the whole session (a new
 * instance per frame would defeat the point). Not thread-safe; intended to
 * be owned and called only by the single inference/analysis thread.</p>
 */
public final class LandmarkSmoother {

    private final Map<KeypointType, OneEuroFilter> xFilters = new EnumMap<>(KeypointType.class);
    private final Map<KeypointType, OneEuroFilter> yFilters = new EnumMap<>(KeypointType.class);

    public LandmarkSmoother(AppConfig config) {
        for (KeypointType type : KeypointType.values()) {
            xFilters.put(type, new OneEuroFilter(
                    config.getSmoothingMinCutoff(), config.getSmoothingBeta(), config.getSmoothingDerivativeCutoff()));
            yFilters.put(type, new OneEuroFilter(
                    config.getSmoothingMinCutoff(), config.getSmoothingBeta(), config.getSmoothingDerivativeCutoff()));
        }
    }

    public PoseResult smooth(PoseResult raw) {
        Map<KeypointType, Landmark> smoothed = new EnumMap<>(KeypointType.class);
        long t = raw.getTimestampNanos();
        for (KeypointType type : KeypointType.values()) {
            Landmark l = raw.get(type);
            if (l == null) {
                continue;
            }
            double sx = xFilters.get(type).filter(l.getX(), t);
            double sy = yFilters.get(type).filter(l.getY(), t);
            smoothed.put(type, new Landmark(type, sx, sy, l.getScore()));
        }
        return new PoseResult(smoothed, t);
    }

    /** Clears all filter state, e.g. after a period with no detected person. */
    public void reset() {
        xFilters.values().forEach(OneEuroFilter::reset);
        yFilters.values().forEach(OneEuroFilter::reset);
    }
}
