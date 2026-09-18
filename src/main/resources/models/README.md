# MoveNet ONNX Model

The application requires the MoveNet SinglePose Lightning model in ONNX format for pose estimation.

## File Details
- **Expected Filename:** `movenet_singlepose_lightning.onnx`
- **Location:** Place the file in this directory (`src/main/resources/models/`) or configure the path in settings.
- **Input:** 192x192 (RGB)
- **Output:** 17 COCO keypoints

## Download & Conversion

1. **TensorFlow Hub:**
   Download the original TFLite model from TensorFlow Hub:
   [MoveNet SinglePose Lightning](https://tfhub.dev/google/movenet/singlepose/lightning/4)

2. **Conversion to ONNX:**
   The TFLite model needs to be converted to ONNX. You can use the `tf2onnx` tool:
   ```bash
   pip install tf2onnx
   python -m tf2onnx.convert --tflite model.tflite --output movenet_singlepose_lightning.onnx
   ```

3. **Alternative:**
   You can also find community-converted ONNX versions of this model on GitHub or HuggingFace.

## License
The model is licensed under the Apache 2.0 License.

## Note on Model Variants
There is also a Thunder variant (256x256 input) which provides higher accuracy but at a slower inference speed. This application uses the Lightning model for optimal real-time performance.
