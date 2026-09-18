package com.fitness.pose;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link PoseEstimator} backed by ONNX Runtime running a pretrained MoveNet
 * SinglePose model (Lightning or Thunder - see the project README for why
 * MoveNet was chosen over MediaPipe/BlazePose and other alternatives for a
 * desktop-Java target).
 *
 * <p><b>Expected model contract</b> (matches the standard MoveNet SinglePose
 * export as published on TensorFlow Hub and mirrored as ONNX by the
 * community - see {@code resources/models/README.md} for exact download
 * links): a single input tensor of shape {@code [1, inputSize, inputSize, 3]},
 * dtype float32, RGB channel order, pixel values in the raw {@code [0,255]}
 * range (NOT pre-normalized to [0,1] or [-1,1] - MoveNet does its own
 * internal normalization); and a single output tensor of shape
 * {@code [1, 1, 17, 3]} where the last axis is {@code (y, x, score)} with y
 * and x normalized to {@code [0,1]} relative to the square input image.</p>
 *
 * <p><b>Important note on verification:</b> this class could not be
 * compiled or exercised against a real model inside the sandboxed
 * environment this project was developed in - that environment has no
 * network access to Maven Central (where the {@code com.microsoft.onnxruntime:onnxruntime}
 * artifact is hosted) and therefore no way to obtain the ONNX Runtime jar
 * or a model file to test against. Every other class in this project was
 * compiled and, where practical, exercised with real tests; this one is the
 * exception, and it is written carefully against ONNX Runtime's documented
 * Java API, but you should treat it as the first thing to sanity-check
 * after cloning (run one frame through it and print the output shape)
 * before trusting it further. See the README's "What Was / Was Not
 * Verified" section.</p>
 */
public final class OnnxMoveNetPoseEstimator implements PoseEstimator {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;
    private final int inputSize;

    public OnnxMoveNetPoseEstimator(String modelPath, int inputSize) throws PoseEstimationException {
        this.inputSize = inputSize;
        Path path = Path.of(modelPath);
        if (!Files.isRegularFile(path)) {
            throw new PoseEstimationException("Pose model file not found at " + modelPath
                    + ". See resources/models/README.md for how to download it.");
        }
        try {
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            this.session = env.createSession(path.toString(), options);
            this.inputName = session.getInputNames().iterator().next();
            this.outputName = session.getOutputNames().iterator().next();
        } catch (OrtException e) {
            throw new PoseEstimationException("Could not load pose model at " + modelPath
                    + ". The file may be corrupted, in an unsupported format, or built for an incompatible "
                    + "ONNX Runtime version.", e);
        }
    }

    @Override
    public PoseResult estimate(RawFrame frame) throws PoseEstimationException {
        FramePreprocessor.PreprocessedFrame pre = FramePreprocessor.letterboxResize(frame, inputSize);

        try {
            FloatBuffer buffer = FloatBuffer.wrap(pre.rgbData);
            long[] shape = {1, inputSize, inputSize, 3};
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape)) {
                try (OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor))) {
                    Optional<OnnxValue> outputValue = result.get(outputName);
                    if (outputValue.isEmpty()) {
                        throw new PoseEstimationException("Pose model produced no output named '" + outputName + "'");
                    }
                    float[][][][] output = (float[][][][]) outputValue.get().getValue();
                    return parseOutput(output, pre, frame.getTimestampNanos());
                }
            }
        } catch (OrtException e) {
            throw new PoseEstimationException("Pose model inference failed: " + e.getMessage()
                    + ". If this is the very first inference call, check that the model file matches the "
                    + "expected MoveNet SinglePose input signature (float32, [1," + inputSize + "," + inputSize + ",3]).", e);
        } catch (ClassCastException e) {
            throw new PoseEstimationException("Unexpected pose model output shape. This estimator expects a "
                    + "MoveNet SinglePose-style output tensor of shape [1,1,17,3] (y, x, score per keypoint). "
                    + "If you're using a different pretrained model, adapt parseOutput() to its output layout.", e);
        }
    }

    private PoseResult parseOutput(float[][][][] output, FramePreprocessor.PreprocessedFrame pre, long timestampNanos) {
        Map<KeypointType, Landmark> landmarks = new EnumMap<>(KeypointType.class);
        KeypointType[] types = KeypointType.values();
        float[][] keypoints = output[0][0]; // [17][3]: (y, x, score) per keypoint
        for (int i = 0; i < types.length && i < keypoints.length; i++) {
            float y = keypoints[i][0];
            float x = keypoints[i][1];
            float score = keypoints[i][2];
            double[] mapped = pre.mapBackToSourceNormalized(x, y);
            landmarks.put(types[i], new Landmark(types[i], mapped[0], mapped[1], score));
        }
        return new PoseResult(landmarks, timestampNanos);
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            // Best-effort cleanup on shutdown; nothing useful to do with this beyond not crashing.
        }
    }
}
