package com.fitness.ui;

import com.fitness.analysis.MovementState;
import com.fitness.workout.WorkoutState;
import com.fitness.workout.WorkoutTick;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The main dashboard: live camera preview with skeleton overlay on the
 * left, session stats / status / feedback on the right, transport controls
 * along the bottom. Built programmatically (no FXML) to keep the project's
 * moving parts - and its resource-path failure modes - to a minimum.
 *
 * <p>This class only builds and updates widgets; it knows nothing about
 * {@code WorkoutManager}, {@code CameraManager}, or threading. All of that
 * lives in {@code Main} and {@code FrameProcessingPipeline}, which call the
 * update methods here from the JavaFX Application Thread only.</p>
 */
public final class MainView {

    public static final int CANVAS_WIDTH = 640;
    public static final int CANVAS_HEIGHT = 480;

    private final BorderPane root = new BorderPane();
    private final Canvas cameraCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);

    private final Label repsValue = new Label("0");
    private final Label setValue = new Label("1");
    private final Label workoutTimeValue = new Label("00:00");
    private final Label activeTimeValue = new Label("00:00");
    private final Label breakTimeValue = new Label("00:00");
    private final Label statusDot = new Label("\u25CF");
    private final Label statusText = new Label("NOT STARTED");
    private final Label feedbackText = new Label("Press Start Workout to begin");
    private final Label cameraStatusText = new Label("");

    private final Button startButton = new Button("Start Workout");
    private final Button pauseButton = new Button("Pause");
    private final Button stopButton = new Button("Stop");
    private final Button resetButton = new Button("Reset");
    private final Button historyButton = new Button("History");
    private final Button settingsButton = new Button("Settings");

    public MainView(Runnable onStart, Runnable onPause, Runnable onResume, Runnable onStop,
                     Runnable onReset, Runnable onHistory, Runnable onSettings) {
        root.getStyleClass().add("root-pane");

        Label title = new Label("AI Workout Tracker");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setPadding(new Insets(12, 0, 12, 16));
        root.setTop(title);

        StackPane cameraPane = new StackPane(cameraCanvas);
        cameraPane.getStyleClass().add("camera-pane");
        cameraPane.setPadding(new Insets(0, 8, 0, 16));
        GraphicsContext gc = cameraCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#12151c"));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(Color.web("#8a93a6"));
        gc.fillText("Waiting for camera...", CANVAS_WIDTH / 2.0 - 70, CANVAS_HEIGHT / 2.0);
        root.setCenter(cameraPane);

        root.setRight(buildStatsPanel());
        root.setBottom(buildControls());

        startButton.setOnAction(e -> onStart.run());
        stopButton.setOnAction(e -> onStop.run());
        resetButton.setOnAction(e -> onReset.run());
        historyButton.setOnAction(e -> onHistory.run());
        settingsButton.setOnAction(e -> onSettings.run());
        pauseButton.setOnAction(e -> {
            if ("Pause".equals(pauseButton.getText())) {
                onPause.run();
            } else {
                onResume.run();
            }
        });

        updateButtonsForState(WorkoutState.NOT_STARTED);
    }

    private VBox buildStatsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(260);
        panel.getStyleClass().add("stats-panel");

        panel.getChildren().add(statRow("Repetitions", repsValue));
        panel.getChildren().add(statRow("Current Set", setValue));
        panel.getChildren().add(statRow("Workout Time", workoutTimeValue));
        panel.getChildren().add(statRow("Active Time", activeTimeValue));
        panel.getChildren().add(statRow("Break Time", breakTimeValue));

        Label statusLabel = new Label("STATUS");
        statusLabel.getStyleClass().add("section-label");
        statusDot.setTextFill(Color.web("#8a93a6"));
        HBox statusRow = new HBox(8, statusDot, statusText);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label feedbackLabel = new Label("FEEDBACK");
        feedbackLabel.getStyleClass().add("section-label");
        feedbackText.setWrapText(true);

        cameraStatusText.getStyleClass().add("camera-status");
        cameraStatusText.setWrapText(true);

        panel.getChildren().addAll(statusLabel, statusRow, feedbackLabel, feedbackText, cameraStatusText);
        return panel;
    }

    private HBox statRow(String label, Label valueLabel) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        HBox.setHgrow(l, Priority.ALWAYS);
        valueLabel.getStyleClass().add("stat-value");
        HBox row = new HBox(l, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildControls() {
        HBox controls = new HBox(10, startButton, pauseButton, stopButton, resetButton, historyButton, settingsButton);
        controls.setPadding(new Insets(12, 16, 16, 16));
        controls.setAlignment(Pos.CENTER_LEFT);
        return controls;
    }

    public Parent getRoot() {
        return root;
    }

    /** Draws the latest camera frame and skeleton overlay. Call only from the FX Application Thread. */
    public void updateFrame(Image frameImage, WorkoutTick tick, double confidenceThreshold) {
        GraphicsContext gc = cameraCanvas.getGraphicsContext2D();
        if (frameImage != null) {
            gc.drawImage(frameImage, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        }
        if (tick != null && tick.getPose() != null) {
            SkeletonRenderer.draw(gc, tick.getPose(), CANVAS_WIDTH, CANVAS_HEIGHT, confidenceThreshold);
        }
    }

    /** Updates all stat labels, the status indicator, and the feedback line. Call only from the FX Application Thread. */
    public void updateTick(WorkoutTick tick) {
        repsValue.setText(String.valueOf(tick.getRepCount()));
        setValue.setText(String.valueOf(tick.getCurrentSetNumber()));
        workoutTimeValue.setText(formatDuration(tick.getTotalWorkoutDurationMs()));
        activeTimeValue.setText(formatDuration(tick.getActiveDurationMs()));
        breakTimeValue.setText(formatDuration(tick.getTotalRestDurationMs()));
        feedbackText.setText(tick.getFeedback());
        applyMovementState(tick.getMovementState());
        updateButtonsForState(tick.getWorkoutState());
    }

    private void applyMovementState(MovementState state) {
        switch (state) {
            case MOVING:
                statusDot.setTextFill(Color.web("#00e5a0"));
                statusText.setText("MOVING");
                break;
            case RESTING:
                statusDot.setTextFill(Color.web("#ffb454"));
                statusText.setText("RESTING");
                break;
            case NO_POSE_DETECTED:
                statusDot.setTextFill(Color.web("#ff6b6b"));
                statusText.setText("NO PERSON DETECTED");
                break;
            case IDLE:
            default:
                statusDot.setTextFill(Color.web("#8a93a6"));
                statusText.setText("IDLE");
                break;
        }
    }

    public void updateButtonsForState(WorkoutState state) {
        boolean notStartedOrStopped = state == WorkoutState.NOT_STARTED || state == WorkoutState.STOPPED;
        boolean activeOrPaused = state == WorkoutState.ACTIVE || state == WorkoutState.PAUSED;

        startButton.setDisable(!notStartedOrStopped);
        pauseButton.setDisable(!activeOrPaused);
        stopButton.setDisable(!activeOrPaused);
        resetButton.setDisable(!notStartedOrStopped);
        pauseButton.setText(state == WorkoutState.PAUSED ? "Resume" : "Pause");
    }

    public void setCameraStatus(String message) {
        cameraStatusText.setText(message == null ? "" : message);
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
