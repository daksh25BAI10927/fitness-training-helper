# Architecture of AI Workout Tracker

## Overview
The AI Workout Tracker is a desktop application built in Java 17, leveraging JavaFX for the UI, OpenCV for camera access, and ONNX Runtime for local ML inference. It uses the MoveNet SinglePose model to extract human pose landmarks in real-time, analyzing these landmarks to count reps, detect sets, and monitor resting periods.

## Pose Pipeline
1. **Camera**: Captures frames via OpenCV.
2. **OpenCV**: Converts hardware camera frames to matrix representations.
3. **RawFrame**: Represents an unprocessed image matrix.
4. **FramePreprocessor**: Resizes, pads, and normalizes frames into a tensor format suitable for the ONNX model.
5. **ONNX MoveNet**: Runs the MoveNet SinglePose model for rapid pose estimation.
6. **PoseResult**: Encapsulates the output of the model (17 keypoints with confidence scores).
7. **LandmarkSmoother**: Applies a One-Euro filter to minimize jitter in coordinate predictions.
8. **Smoothed PoseResult**: Output passed to movement analysis algorithms.

## Landmark Representation
The model identifies 17 COCO keypoints:
- Coordinates are normalized to (0, 1) range based on the frame dimensions.
- Each keypoint comes with a confidence score. Low-confidence keypoints are omitted from composite signals.

## Movement Analysis
- **Body-scale Normalization**: Adjusts raw distances by the person's size (e.g., shoulder width or torso length) to create depth-independent metrics.
- **Signals**: Extracts leg extension and arm extension metrics.
- **AdaptiveBaseline**: Subtracts resting baselines to create a zero-centered composite signal.
- **Composite Signal**: A unified scalar representing the phase of movement.

## Rep Counting
- Peak/valley detection on the composite signal.
- A rep requires:
  - Exceeding a prominence threshold.
  - Exceeding a minimum rep duration.
  - A rep cooldown to prevent double-counting.

## Set Detection
- Closes a set automatically when:
  - Resting time exceeds `setBreakThresholdMs`.
  - The number of reps counted meets or exceeds `minRepsPerSet`.

## Rest Detection
- Identifies periods of low movement by comparing the absolute value of the composite signal against a low-activity threshold.
- Hysteresis is applied to avoid toggling rapidly between active and resting states.

## Threading Model
- **Camera Thread**: Continuously pulls frames from the camera into a bounded queue.
- **Processing Thread**: Polls frames, preprocesses, runs inference, analyzes movement, updates states, and delegates UI updates via `Platform.runLater()`.
- **UI Thread**: Handles rendering the JavaFX interface and the Canvas stream smoothly.

## UI Architecture
- No FXML is used; the UI is built entirely via Java code for maintainability.
- **MainView**: The primary window for tracking.
- **Canvas**: Efficiently draws the camera feed and overlaid pose skeletons.
- **HistoryView**: A dialog that fetches and displays past workouts.

## Database Structure
Local SQLite database stored in the config directory.
- `sessions` table: stores high-level workout session metadata (duration, total reps).
- `session_sets` table: stores individual set breakdowns, utilizing a cascade delete constraint to clean up sets when a session is removed.

## Configuration
Application parameters (camera index, thresholds, paths) are stored in `~/.ai-workout-tracker/config.properties`.
