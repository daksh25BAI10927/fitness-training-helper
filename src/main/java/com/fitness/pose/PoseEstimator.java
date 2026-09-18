package com.fitness.pose;

/**
 * Abstraction over whatever pretrained pose-estimation backend is actually
 * running inference. The rest of the application (movement analysis,
 * rep/set/rest detection, UI) depends only on this interface and on
 * {@link PoseResult} / {@link Landmark} - never on ONNX Runtime, OpenCV, or
 * any other ML-runtime type. This is what let us swap in a different model
 * or runtime later without touching analysis or UI code.
 */
public interface PoseEstimator extends AutoCloseable {

    /**
     * Runs pose estimation on a single frame.
     *
     * @param frame the captured camera frame
     * @return the detected landmarks (always 17 entries; low-confidence
     *         landmarks are still returned, with a low score, so the caller
     *         can decide what "visible" means)
     * @throws PoseEstimationException if inference fails
     */
    PoseResult estimate(RawFrame frame) throws PoseEstimationException;

    /** Releases native resources (ONNX Runtime session/environment, etc). */
    @Override
    void close();
}
