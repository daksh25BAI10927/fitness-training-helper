# AI Real-Time Workout & Movement Tracker

A desktop application that uses real-time pose estimation to automatically detect and track repetitive physical movements, count repetitions, detect sets and rest periods, and maintain workout statistics — all without requiring the user to select an exercise type.

## Overview

This application combines computer vision (OpenCV + MoveNet pose estimation via ONNX Runtime) with a JavaFX desktop GUI to provide an intelligent workout companion. Point your webcam at yourself, press **Start Workout**, and the system automatically:

- Detects your body pose in real time (17 COCO keypoints)
- Overlays a skeleton visualization on the live camera feed
- Tracks repetitive movements and counts reps
- Detects when you're resting between sets
- Automatically groups reps into sets
- Provides real-time movement feedback
- Saves complete session history to a local database

> **Important:** This is a *generic repetitive movement tracker*, not an exercise classifier. It tracks any repetitive body movement (squats, curls, presses, lunges, etc.) without needing to know which exercise you're performing. See [Known Limitations](#known-limitations) for details.

## Features

- **Real-Time Pose Detection** — MoveNet SinglePose Lightning via ONNX Runtime, running locally on CPU
- **Skeleton Overlay** — Color-coded landmark and bone visualization on the camera feed
- **Generic Rep Counting** — Peak/valley detection on a body-scale-normalized movement signal
- **Automatic Set Detection** — Sets are inferred from sustained rest periods between movement
- **Rest/Break Tracking** — Total rest time, current rest duration, longest rest, rest count
- **Workout Timers** — Total workout time, active movement time, break time
- **Movement Feedback** — Context-aware messages based on pose quality and movement
- **Workout History** — SQLite-backed session storage with full metrics and per-set breakdown
- **Settings Panel** — Adjustable sensitivity, thresholds, camera settings, smoothing parameters
- **Dark Theme UI** — Professional JavaFX interface
- **Offline Operation** — Everything runs locally after initial setup
- **Privacy-First** — No camera data leaves your machine

## Architecture

```
┌──────────────┐    ┌──────────────┐    ┌────────────────────┐    ┌──────────┐
│ Camera Thread │───>│  Frame Queue │───>│  Processing Thread  │───>│ UI Thread│
│  (OpenCV)    │    │  (bounded)   │    │  Pose + Analysis    │    │ (JavaFX) │
└──────────────┘    └──────────────┘    └────────────────────┘    └──────────┘
                                              │
                                    ┌─────────┴──────────┐
                                    │                    │
                               ┌────┴─────┐     ┌───────┴────────┐
                               │  MoveNet │     │ Movement       │
                               │  (ONNX)  │     │ Analysis       │
                               └──────────┘     │ Pipeline       │
                                                │                │
                                                │ Smoothing      │
                                                │ → Movement     │
                                                │ → Rep Counter  │
                                                │ → Set Detector │
                                                │ → Rest Detector│
                                                │ → Feedback     │
                                                └────────────────┘
```

### Movement Analysis Pipeline

```
Raw Pose (17 landmarks)
     │
     ▼
One Euro Filter (per-landmark smoothing)
     │
     ▼
Body-Scale Normalization (torso length)
     │
     ▼
Leg Extension Signal ──────┐
                           ├──> Signed Composite Signal
Arm Extension Signal ──────┘
     │
     ▼
Adaptive Baseline (slow/fast EMA)
     │
     ▼
Peak/Valley Detection (prominence + hysteresis)
     │
     ▼
Rep Count
     │
     ▼
Rest Detection (inactivity threshold)
     │
     ▼
Set Detection (rest duration + min reps)
```

## Technologies

| Technology | Version | Purpose |
|---|---|---|
| Java | 17+ | Application runtime |
| JavaFX | 21 | Desktop UI framework |
| OpenCV | 4.9.0 | Webcam capture |
| ONNX Runtime | 1.17.x | ML inference engine |
| MoveNet Lightning | SinglePose v4 | Pretrained pose model |
| SQLite | 3.45+ | Local workout history |
| Maven | 3.8+ | Build system |

## ML Model: Why MoveNet?

After evaluating multiple pose estimation solutions for a desktop Java target:

| Model | Java Compat. | Desktop JVM | Maven Avail. | Latency | Landmarks | Deployment |
|---|---|---|---|---|---|---|
| **MoveNet Lightning (ONNX)** | ✅ via ONNX Runtime | ✅ | ✅ | ~15ms CPU | 17 COCO | Simple |
| MediaPipe Pose | ❌ No Java API | ❌ | ❌ | ~10ms | 33 | Complex |
| MoveNet Thunder (ONNX) | ✅ | ✅ | ✅ | ~40ms CPU | 17 COCO | Simple |
| TensorFlow Lite | ⚠️ Limited | ⚠️ | ❌ | ~20ms | 17 | Complex |

**MoveNet Lightning via ONNX Runtime** was chosen because:
- ONNX Runtime has a first-class, well-maintained Java API on Maven Central
- MoveNet Lightning offers excellent accuracy/speed tradeoff for real-time desktop use
- The ONNX format is deployment-friendly (single `.onnx` file)
- Runs entirely on CPU with no GPU dependencies
- Apache 2.0 licensed
- Works fully offline after model download

## Installation

### Prerequisites

1. **Java 17+** (JDK, not just JRE)
   ```bash
   java -version    # Must show 17 or higher
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   ```

3. **A webcam** connected to your computer

### Setup

```bash
# 1. Clone the repository
git clone <repository-url>
cd fitness-tracker

# 2. Download the MoveNet ONNX model
# See src/main/resources/models/README.md for detailed instructions
# Place movenet_singlepose_lightning.onnx in src/main/resources/models/

# 3. Build the project
mvn clean install -DskipTests

# 4. Run the application
mvn javafx:run
```

### Model Setup

The application requires a MoveNet SinglePose Lightning model in ONNX format.

**Option A — Download a pre-converted ONNX model** (recommended):
Search for "movenet singlepose lightning onnx" on GitHub or HuggingFace.

**Option B — Convert from TensorFlow Hub**:
```bash
pip install tf2onnx tensorflow
python -m tf2onnx.convert \
    --tflite movenet_singlepose_lightning.tflite \
    --output src/main/resources/models/movenet_singlepose_lightning.onnx
```

See [`src/main/resources/models/README.md`](src/main/resources/models/README.md) for detailed download and conversion instructions.

## Configuration

Settings are stored in `~/.ai-workout-tracker/config.properties` and can be changed via the in-app Settings panel.

| Setting | Default | Description |
|---|---|---|
| `camera.index` | `0` | Webcam device index |
| `camera.width` | `640` | Capture width (px) |
| `camera.height` | `480` | Capture height (px) |
| `camera.fps` | `30` | Capture frame rate |
| `pose.confidenceThreshold` | `0.30` | Min keypoint confidence |
| `movement.sensitivity` | `0.5` | Rep detection sensitivity (0-1) |
| `movement.minRepDurationMs` | `250` | Min rep duration |
| `movement.repCooldownMs` | `200` | Cooldown between reps |
| `rest.entryThresholdMs` | `3000` | Inactivity before "resting" |
| `rest.setBreakThresholdMs` | `8000` | Inactivity to close a set |
| `smoothing.minCutoff` | `1.2` | One Euro Filter min cutoff |
| `smoothing.beta` | `0.02` | One Euro Filter speed coeff. |

## Running the Application

```bash
# Standard run
mvn javafx:run

# Run with specific Java version
JAVA_HOME=/path/to/jdk17 mvn javafx:run

# Build and run the JAR directly (after mvn package)
java --module-path <javafx-sdk>/lib \
     --add-modules javafx.controls,javafx.graphics \
     -jar target/ai-workout-tracker-1.0.0.jar
```

## Usage

1. **Launch** the application — the camera preview starts automatically
2. **Position yourself** so your full body is visible in the frame
3. **Press "Start Workout"** to begin tracking
4. **Exercise** — the system will automatically count reps and detect rest periods
5. **Press "Stop"** when done — your session is saved to history
6. **View History** to see past workout sessions
7. **Adjust Settings** if the sensitivity needs tuning for your movements

## Screenshots

*[Screenshots to be added after first successful run]*

## Troubleshooting

| Problem | Solution |
|---|---|
| "Could not open camera" | Check webcam connection; close other apps using it; try a different camera index in Settings |
| "Pose model file not found" | Download the ONNX model — see [Model Setup](#model-setup) |
| No skeleton overlay | Ensure full body is visible; improve lighting; lower confidence threshold |
| Too many/few reps counted | Adjust Movement Sensitivity in Settings (higher = more sensitive) |
| Application won't start | Verify Java 17+ and Maven are installed; run `mvn clean install` |
| OpenCV error | The bundled `org.openpnp:opencv` should include native libs; if not, install OpenCV system-wide |
| JavaFX module errors | Ensure you're using `mvn javafx:run` not `mvn exec:java` |

## Privacy

- **All camera data stays local.** No frames, poses, or workout data are ever transmitted over the network.
- The application works fully offline after the initial model download.
- Workout history is stored in a local SQLite database on your machine.
- Camera access is clearly indicated in the UI.

## Known Limitations

1. **Generic movement tracking, not exercise identification.** The system detects repetitive body movement but cannot identify specific exercises. It works best with exercises that involve clear, cyclic movement of the limbs (squats, curls, presses, lunges, etc.).

2. **Single person only.** MoveNet SinglePose tracks one person. If multiple people are visible, results may be unpredictable.

3. **Full-body visibility required.** The system needs to see at least your shoulders, hips, and relevant limbs. Floor exercises or close-up exercises may not track well.

4. **Lighting dependent.** Pose estimation accuracy degrades in poor lighting or high-contrast conditions.

5. **CPU-bound performance.** Runs on CPU only. On older hardware, you may need to increase `inferenceEveryNFrames` to maintain UI responsiveness.

6. **Not medically validated.** This is a movement tracking tool, not a medical device. Do not rely on it for rehabilitation, injury prevention, or any medical purpose.

7. **Small movements may be missed.** Very subtle exercises (like wrist rotations or isolated finger movements) won't generate enough signal to be detected.

## Project Structure

```
AI-Workout-Tracker/
├── pom.xml                              # Maven build configuration
├── README.md                            # This file
├── docs/
│   └── ARCHITECTURE.md                  # Developer documentation
├── src/
│   ├── main/
│   │   ├── java/com/fitness/
│   │   │   ├── Main.java                # JavaFX Application entry point
│   │   │   ├── camera/                  # Webcam capture
│   │   │   │   ├── CameraManager.java
│   │   │   │   └── CameraException.java
│   │   │   ├── pose/                    # ML pose estimation
│   │   │   │   ├── PoseEstimator.java
│   │   │   │   ├── OnnxMoveNetPoseEstimator.java
│   │   │   │   ├── FramePreprocessor.java
│   │   │   │   ├── RawFrame.java
│   │   │   │   ├── PoseResult.java
│   │   │   │   ├── Landmark.java
│   │   │   │   ├── KeypointType.java
│   │   │   │   └── PoseEstimationException.java
│   │   │   ├── analysis/                # Movement analysis pipeline
│   │   │   │   ├── MovementAnalyzer.java
│   │   │   │   ├── RepCounter.java
│   │   │   │   ├── RestDetector.java
│   │   │   │   ├── SetDetector.java
│   │   │   │   ├── FeedbackGenerator.java
│   │   │   │   ├── LandmarkSmoother.java
│   │   │   │   ├── OneEuroFilter.java
│   │   │   │   ├── MovementSample.java
│   │   │   │   ├── MovementState.java
│   │   │   │   └── SetRecord.java
│   │   │   ├── workout/                 # Workout session management
│   │   │   │   ├── WorkoutManager.java
│   │   │   │   ├── TimerManager.java
│   │   │   │   ├── WorkoutSession.java
│   │   │   │   ├── WorkoutTick.java
│   │   │   │   └── WorkoutState.java
│   │   │   ├── database/                # SQLite persistence
│   │   │   │   ├── DatabaseManager.java
│   │   │   │   ├── SessionRecord.java
│   │   │   │   └── DatabaseException.java
│   │   │   ├── ui/                      # JavaFX UI
│   │   │   │   ├── MainView.java
│   │   │   │   ├── HistoryView.java
│   │   │   │   ├── SkeletonRenderer.java
│   │   │   │   └── SettingsView.java
│   │   │   ├── config/                  # Configuration
│   │   │   │   ├── AppConfig.java
│   │   │   │   └── ConfigManager.java
│   │   │   └── utils/                   # Utilities
│   │   │       ├── ImageConverter.java
│   │   │       └── AppLogger.java
│   │   └── resources/
│   │       ├── css/style.css            # Dark theme stylesheet
│   │       └── models/
│   │           └── README.md            # Model download instructions
│   └── test/java/com/fitness/          # Unit tests
│       ├── analysis/
│       │   ├── RepCounterTest.java
│       │   ├── RestDetectorTest.java
│       │   ├── SetDetectorTest.java
│       │   └── OneEuroFilterTest.java
│       ├── workout/
│       │   └── TimerManagerTest.java
│       └── database/
│           └── DatabaseManagerTest.java
```

## Testing

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=RepCounterTest

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

Tests cover:
- Rep counting accuracy (clean signals, noise rejection, edge cases)
- Rest detection (threshold crossing, hysteresis)
- Set detection (set boundaries, minimum reps)
- One Euro Filter (smoothing, convergence, step response)
- Timer management (start/pause/resume/stop lifecycle)
- Database operations (save, load, delete, schema creation)

## Future Improvements

- **GPU acceleration** via ONNX Runtime CUDA/DirectML providers
- **Exercise classification** using temporal movement patterns
- **Rep quality scoring** based on movement consistency
- **Progress charts** showing workout trends over time
- **Export functionality** (CSV, JSON)
- **Multi-person tracking** via MoveNet MultiPose
- **Audio feedback** for rep counting
- **Camera selection UI** with preview
- **Packaging** as native installer (jpackage)
- **MoveNet Thunder** option for higher accuracy

## Licenses & Attributions

| Component | License |
|---|---|
| MoveNet | Apache 2.0 |
| ONNX Runtime | MIT |
| OpenCV | Apache 2.0 |
| JavaFX (OpenJFX) | GPL v2 + Classpath Exception |
| SQLite JDBC | Apache 2.0 |
| One Euro Filter | Based on Casiez, Roussel & Vogel (2012) |

## Authors

Built as a desktop AI fitness tracking application demonstrating real-time pose estimation, signal processing, and JavaFX desktop development.
