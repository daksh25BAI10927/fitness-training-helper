package com.fitness.ui;

import com.fitness.pose.KeypointType;
import com.fitness.pose.Landmark;
import com.fitness.pose.PoseResult;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Draws landmarks and bone connections onto a {@link GraphicsContext}, color-coded by confidence. */
public final class SkeletonRenderer {

    private static final double LANDMARK_RADIUS = 5.0;
    private static final Color HIGH_CONFIDENCE_COLOR = Color.web("#00e5a0");
    private static final Color LOW_CONFIDENCE_COLOR = Color.web("#ff6b6b");
    private static final Color BONE_COLOR = Color.web("#00e5a0", 0.6);

    private SkeletonRenderer() {}

    public static void draw(GraphicsContext gc, PoseResult pose, double canvasWidth, double canvasHeight,
                             double confidenceThreshold) {
        if (pose == null) {
            return;
        }

        gc.setLineWidth(3.0);
        gc.setStroke(BONE_COLOR);
        for (int[] edge : KeypointType.SKELETON_EDGES) {
            Landmark a = pose.get(KeypointType.values()[edge[0]]);
            Landmark b = pose.get(KeypointType.values()[edge[1]]);
            if (a == null || b == null) continue;
            if (!a.isVisible(confidenceThreshold) || !b.isVisible(confidenceThreshold)) continue;
            gc.strokeLine(a.getX() * canvasWidth, a.getY() * canvasHeight, b.getX() * canvasWidth, b.getY() * canvasHeight);
        }

        for (Landmark l : pose.allLandmarks().values()) {
            gc.setFill(l.isVisible(confidenceThreshold) ? HIGH_CONFIDENCE_COLOR : LOW_CONFIDENCE_COLOR);
            double px = l.getX() * canvasWidth;
            double py = l.getY() * canvasHeight;
            gc.fillOval(px - LANDMARK_RADIUS, py - LANDMARK_RADIUS, LANDMARK_RADIUS * 2, LANDMARK_RADIUS * 2);
        }
    }
}
