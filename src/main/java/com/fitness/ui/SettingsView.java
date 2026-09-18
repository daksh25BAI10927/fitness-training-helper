package com.fitness.ui;

import com.fitness.config.AppConfig;
import com.fitness.config.ConfigManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;

/**
 * A modal dialog for adjusting application settings. Reads from and writes to
 * {@link AppConfig} via {@link ConfigManager}, using the exact getter/setter
 * names defined on {@code AppConfig}.
 */
public final class SettingsView {

    private SettingsView() {}

    /**
     * Shows the settings dialog.
     *
     * @param owner         The parent window.
     * @param configManager The ConfigManager for persisting settings.
     * @param config        The current AppConfig to populate and modify.
     */
    public static void show(Window owner, ConfigManager configManager, AppConfig config) {
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Settings");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root-pane");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(15);
        grid.setPadding(new Insets(10));

        int row = 0;

        // ---- Camera section ----
        Label cameraLabel = new Label("CAMERA");
        cameraLabel.getStyleClass().add("section-label");
        grid.add(cameraLabel, 0, row++, 2, 1);

        Spinner<Integer> cameraIndex = new Spinner<>(0, 9, config.getCameraIndex());
        grid.add(new Label("Camera Index:"), 0, row);
        grid.add(cameraIndex, 1, row++);

        TextField cameraWidth = new TextField(String.valueOf(config.getCameraWidth()));
        grid.add(new Label("Camera Width:"), 0, row);
        grid.add(cameraWidth, 1, row++);

        TextField cameraHeight = new TextField(String.valueOf(config.getCameraHeight()));
        grid.add(new Label("Camera Height:"), 0, row);
        grid.add(cameraHeight, 1, row++);

        Spinner<Integer> cameraFps = new Spinner<>(10, 60, config.getCameraFps());
        grid.add(new Label("Camera FPS:"), 0, row);
        grid.add(cameraFps, 1, row++);

        grid.add(new Label(), 0, row++); // spacer

        // ---- Movement Detection section ----
        Label movementLabel = new Label("MOVEMENT DETECTION");
        movementLabel.getStyleClass().add("section-label");
        grid.add(movementLabel, 0, row++, 2, 1);

        Slider movementSensitivity = new Slider(0.0, 1.0, config.getMovementSensitivity());
        movementSensitivity.setShowTickMarks(true);
        movementSensitivity.setShowTickLabels(true);
        movementSensitivity.setMajorTickUnit(0.1);
        grid.add(new Label("Movement Sensitivity:"), 0, row);
        grid.add(movementSensitivity, 1, row++);

        Spinner<Integer> minRepDuration = new Spinner<>(100, 2000, (int) config.getMinRepDurationMs());
        minRepDuration.setEditable(true);
        grid.add(new Label("Min Rep Duration (ms):"), 0, row);
        grid.add(minRepDuration, 1, row++);

        Spinner<Integer> repCooldown = new Spinner<>(50, 1000, (int) config.getRepCooldownMs());
        repCooldown.setEditable(true);
        grid.add(new Label("Rep Cooldown (ms):"), 0, row);
        grid.add(repCooldown, 1, row++);

        Spinner<Integer> minRepsPerSet = new Spinner<>(1, 20, config.getMinRepsPerSet());
        grid.add(new Label("Min Reps Per Set:"), 0, row);
        grid.add(minRepsPerSet, 1, row++);

        grid.add(new Label(), 0, row++); // spacer

        // ---- Rest Detection section ----
        Label restLabel = new Label("REST DETECTION");
        restLabel.getStyleClass().add("section-label");
        grid.add(restLabel, 0, row++, 2, 1);

        Spinner<Integer> restEntryThreshold = new Spinner<>(1000, 10000, (int) config.getRestEntryThresholdMs());
        restEntryThreshold.setEditable(true);
        grid.add(new Label("Rest Entry Threshold (ms):"), 0, row);
        grid.add(restEntryThreshold, 1, row++);

        Spinner<Integer> setBreakThreshold = new Spinner<>(3000, 30000, (int) config.getSetBreakThresholdMs());
        setBreakThreshold.setEditable(true);
        grid.add(new Label("Set Break Threshold (ms):"), 0, row);
        grid.add(setBreakThreshold, 1, row++);

        grid.add(new Label(), 0, row++); // spacer

        // ---- Pose Estimation section ----
        Label poseLabel = new Label("POSE ESTIMATION");
        poseLabel.getStyleClass().add("section-label");
        grid.add(poseLabel, 0, row++, 2, 1);

        Slider confidenceThreshold = new Slider(0.1, 0.9, config.getPoseConfidenceThreshold());
        confidenceThreshold.setShowTickMarks(true);
        confidenceThreshold.setShowTickLabels(true);
        confidenceThreshold.setMajorTickUnit(0.1);
        grid.add(new Label("Confidence Threshold:"), 0, row);
        grid.add(confidenceThreshold, 1, row++);

        Spinner<Integer> inferenceInterval = new Spinner<>(1, 10, config.getInferenceEveryNFrames());
        grid.add(new Label("Inference Every N Frames:"), 0, row);
        grid.add(inferenceInterval, 1, row++);

        grid.add(new Label(), 0, row++); // spacer

        // ---- Smoothing section ----
        Label smoothingLabel = new Label("SMOOTHING (One Euro Filter)");
        smoothingLabel.getStyleClass().add("section-label");
        grid.add(smoothingLabel, 0, row++, 2, 1);

        TextField minCutoff = new TextField(String.valueOf(config.getSmoothingMinCutoff()));
        grid.add(new Label("Min Cutoff (Hz):"), 0, row);
        grid.add(minCutoff, 1, row++);

        TextField beta = new TextField(String.valueOf(config.getSmoothingBeta()));
        grid.add(new Label("Speed Coefficient (Beta):"), 0, row);
        grid.add(beta, 1, row++);

        scrollPane.setContent(grid);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // ---- Buttons ----
        Button saveBtn = new Button("Save");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            try {
                int w = Integer.parseInt(cameraWidth.getText().trim());
                int h = Integer.parseInt(cameraHeight.getText().trim());
                double cutoff = Double.parseDouble(minCutoff.getText().trim());
                double b = Double.parseDouble(beta.getText().trim());

                config.setCameraIndex(cameraIndex.getValue());
                config.setCameraWidth(w);
                config.setCameraHeight(h);
                config.setCameraFps(cameraFps.getValue());

                config.setMovementSensitivity(movementSensitivity.getValue());
                config.setMinRepDurationMs(minRepDuration.getValue());
                config.setRepCooldownMs(repCooldown.getValue());
                config.setMinRepsPerSet(minRepsPerSet.getValue());

                config.setRestEntryThresholdMs(restEntryThreshold.getValue());
                config.setSetBreakThresholdMs(setBreakThreshold.getValue());

                config.setPoseConfidenceThreshold(confidenceThreshold.getValue());
                config.setInferenceEveryNFrames(inferenceInterval.getValue());

                config.setSmoothingMinCutoff(cutoff);
                config.setSmoothingBeta(b);

                configManager.save(config);
                stage.close();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid numeric value. Check Width, Height, Cutoff, and Beta fields.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> stage.close());

        Button resetBtn = new Button("Reset Defaults");
        resetBtn.setOnAction(e -> {
            AppConfig defaults = new AppConfig();
            cameraIndex.getValueFactory().setValue(defaults.getCameraIndex());
            cameraWidth.setText(String.valueOf(defaults.getCameraWidth()));
            cameraHeight.setText(String.valueOf(defaults.getCameraHeight()));
            cameraFps.getValueFactory().setValue(defaults.getCameraFps());

            movementSensitivity.setValue(defaults.getMovementSensitivity());
            minRepDuration.getValueFactory().setValue((int) defaults.getMinRepDurationMs());
            repCooldown.getValueFactory().setValue((int) defaults.getRepCooldownMs());
            minRepsPerSet.getValueFactory().setValue(defaults.getMinRepsPerSet());

            restEntryThreshold.getValueFactory().setValue((int) defaults.getRestEntryThresholdMs());
            setBreakThreshold.getValueFactory().setValue((int) defaults.getSetBreakThresholdMs());

            confidenceThreshold.setValue(defaults.getPoseConfidenceThreshold());
            inferenceInterval.getValueFactory().setValue(defaults.getInferenceEveryNFrames());

            minCutoff.setText(String.valueOf(defaults.getSmoothingMinCutoff()));
            beta.setText(String.valueOf(defaults.getSmoothingBeta()));

            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });

        HBox btnBox = new HBox(15, resetBtn, cancelBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(scrollPane, errorLabel, btnBox);

        Scene scene = new Scene(root, 500, 660);
        URL cssUrl = SettingsView.class.getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
    }
}
