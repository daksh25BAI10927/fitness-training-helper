package com.fitness.pose;

/**
 * A single body keypoint produced by the pose model, in normalized image
 * coordinates ([0,1] x [0,1], origin top-left) plus a confidence score.
 *
 * <p>Instances are immutable value objects so they can be safely shared
 * between the inference thread and the analysis/UI code that reads them.</p>
 */
public final class Landmark {

    private final KeypointType type;
    private final double x;
    private final double y;
    private final double score;

    public Landmark(KeypointType type, double x, double y, double score) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.score = score;
    }

    public KeypointType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getScore() { return score; }

    public boolean isVisible(double confidenceThreshold) {
        return score >= confidenceThreshold;
    }

    public double distanceTo(Landmark other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("%s(x=%.3f, y=%.3f, score=%.2f)", type, x, y, score);
    }
}
