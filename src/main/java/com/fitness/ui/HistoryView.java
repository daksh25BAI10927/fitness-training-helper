package com.fitness.ui;

import com.fitness.database.DatabaseException;
import com.fitness.database.DatabaseManager;
import com.fitness.database.SessionRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A standalone window showing saved workout sessions, newest first, with per-row delete. */
public final class HistoryView {

    private static final Logger LOG = Logger.getLogger(HistoryView.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private HistoryView() {}

    public static void show(Window owner, DatabaseManager db) {
        Stage stage = new Stage();
        stage.setTitle("Workout History");
        stage.initModality(Modality.NONE);
        if (owner != null) {
            stage.initOwner(owner);
        }

        TableView<SessionRecord> table = new TableView<>();
        table.getColumns().addAll(
                column("Date", 150, r -> DATE_FORMAT.format(Instant.ofEpochMilli(r.getStartTimeEpochMillis()).atZone(ZoneId.systemDefault()))),
                column("Reps", 60, r -> String.valueOf(r.getTotalReps())),
                column("Sets", 60, r -> String.valueOf(r.getTotalSets())),
                column("Duration", 90, r -> formatDuration(r.getWorkoutDurationMs())),
                column("Active", 90, r -> formatDuration(r.getActiveDurationMs())),
                column("Breaks", 60, r -> String.valueOf(r.getBreakCount())),
                column("Longest Break", 100, r -> formatDuration(r.getLongestRestMs())),
                column("Avg Rep", 80, r -> r.getAverageRepDurationMs() + " ms"));

        ObservableList<SessionRecord> items = FXCollections.observableArrayList();
        table.setItems(items);

        Label emptyLabel = new Label("No workouts saved yet. Finish a workout to see it here.");
        table.setPlaceholder(emptyLabel);

        Runnable refresh = () -> {
            try {
                items.setAll(db.listSessions());
            } catch (DatabaseException e) {
                LOG.log(Level.WARNING, "Could not load workout history", e);
                showError("Could not load workout history: " + e.getMessage());
            }
        };
        refresh.run();

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
                deleteButton.setDisable(newSel == null));
        deleteButton.setOnAction(e -> {
            SessionRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete this workout session? This cannot be undone.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Delete Session");
            confirm.setHeaderText(null);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    db.deleteSession(selected.getId());
                    refresh.run();
                } catch (DatabaseException ex) {
                    LOG.log(Level.WARNING, "Could not delete session " + selected.getId(), ex);
                    showError("Could not delete session: " + ex.getMessage());
                }
            }
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refresh.run());

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());

        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(10, refreshButton, deleteButton, closeButton);
        buttons.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(table);
        root.setBottom(buttons);
        root.setPadding(new Insets(4));

        stage.setScene(new Scene(root, 760, 420));
        stage.show();
    }

    private static TableColumn<SessionRecord, String> column(String title, double width,
                                                               java.util.function.Function<SessionRecord, String> extractor) {
        TableColumn<SessionRecord, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(extractor.apply(data.getValue())));
        return col;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
