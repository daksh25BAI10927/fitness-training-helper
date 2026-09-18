package com.fitness.pose;

/**
 * The 17 COCO-style body keypoints produced by MoveNet (and most other
 * lightweight pose models). Declared in the exact order MoveNet emits them
 * in its output tensor, so {@code KeypointType.values()[i]} maps directly to
 * output index {@code i}.
 */
public enum KeypointType {
    NOSE,
    LEFT_EYE,
    RIGHT_EYE,
    LEFT_EAR,
    RIGHT_EAR,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_ELBOW,
    RIGHT_ELBOW,
    LEFT_WRIST,
    RIGHT_WRIST,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_ANKLE,
    RIGHT_ANKLE;

    public static final int COUNT = 17;

    /** Bone connections used for skeleton rendering (pairs of keypoint indices). */
    public static final int[][] SKELETON_EDGES = {
            {NOSE.ordinal(), LEFT_EYE.ordinal()},
            {NOSE.ordinal(), RIGHT_EYE.ordinal()},
            {LEFT_EYE.ordinal(), LEFT_EAR.ordinal()},
            {RIGHT_EYE.ordinal(), RIGHT_EAR.ordinal()},
            {LEFT_SHOULDER.ordinal(), RIGHT_SHOULDER.ordinal()},
            {LEFT_SHOULDER.ordinal(), LEFT_ELBOW.ordinal()},
            {LEFT_ELBOW.ordinal(), LEFT_WRIST.ordinal()},
            {RIGHT_SHOULDER.ordinal(), RIGHT_ELBOW.ordinal()},
            {RIGHT_ELBOW.ordinal(), RIGHT_WRIST.ordinal()},
            {LEFT_SHOULDER.ordinal(), LEFT_HIP.ordinal()},
            {RIGHT_SHOULDER.ordinal(), RIGHT_HIP.ordinal()},
            {LEFT_HIP.ordinal(), RIGHT_HIP.ordinal()},
            {LEFT_HIP.ordinal(), LEFT_KNEE.ordinal()},
            {LEFT_KNEE.ordinal(), LEFT_ANKLE.ordinal()},
            {RIGHT_HIP.ordinal(), RIGHT_KNEE.ordinal()},
            {RIGHT_KNEE.ordinal(), RIGHT_ANKLE.ordinal()},
    };
}
