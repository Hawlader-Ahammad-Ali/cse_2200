package com.mindmap.view.source;

import com.mindmap.model.Source;
import com.mindmap.model.SourceStatus;
import com.mindmap.model.SourceType;
import com.mindmap.util.AppContext;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Modal dialog for adding or editing a source.
 *
 * <p>Pure UI: it builds a {@link Source} object and returns it.
 * The caller (SourcesView) invokes {@code SourceService} to persist it,
 * so all DB/validation messages flow through one place.</p>
 */
public class SourceFormDialog {

    private final Dialog<Source> dialog = new Dialog<>();
    private final Source editing;          // null for "Add"

    // Form fields
    private final TextField       titleField       = new TextField();
    private final ComboBox<SourceType> typeCombo   = new ComboBox<>();
    private final TextField       authorField      = new TextField();
    private final TextArea        descriptionField = new TextArea();
    private final TextField       urlField         = new TextField();
    private final ComboBox<SourceStatus> statusCombo = new ComboBox<>();
    private final ComboBox<Integer>    ratingCombo = new ComboBox<>();
    private final DatePicker      dateConsumedPicker = new DatePicker();
    private final TextField       tagsField        = new TextField();
    private final TextArea        notesField       = new TextArea();

    private final Label           errorLabel       = new Label();

    public SourceFormDialog(Source editing) {
        this.editing = editing;
        dialog.setTitle(editing == null ? "Add Source" : "Edit Source");
        dialog.setResizable(true);

        buildUI();
        if (editing != null) populateFields(editing);

        dialog.setResultConverter(bt -> {
            if (bt == null || bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            return buildSourceFromFields();
        });

        // Prevent OK from closing on validation failure
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.addEventFilter(ActionEvent.ACTION, e -> {
                String error = clientValidate();
                if (error != null) {
                    showError(error);
                    e.consume();
                }
            });
        }
    }

    /** Shows the dialog and returns the entered source (empty if cancelled). */
    public Optional<Source> showAndWait() {
        return dialog.showAndWait();
    }

    // ------------------------------------------------------------- UI

    private void buildUI() {
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        applyStylesheet(pane);

        // Basic fields
        titleField.setPromptText("e.g. The Social Network");
        typeCombo.getItems().addAll(SourceType.values());
        typeCombo.setValue(SourceType.MOVIE);
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        authorField.setPromptText("Author, director, creator, or host");

        descriptionField.setPromptText("Short description of the source");
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        urlField.setPromptText("https://…");

        statusCombo.getItems().addAll(SourceStatus.values());
        statusCombo.setValue(SourceStatus.PLANNED);
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setPromptText("Not rated");
        ratingCombo.setMaxWidth(Double.MAX_VALUE);

        dateConsumedPicker.setPromptText("Date consumed (optional)");
        dateConsumedPicker.setMaxWidth(Double.MAX_VALUE);

        tagsField.setPromptText("business, psychology, tech   (comma-separated)");

        notesField.setPromptText("Any quick notes about this source");
        notesField.setPrefRowCount(3);
        notesField.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 4, 4, 4));

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(110);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        int row = 0;
        addRow(grid, row++, "Title *",       titleField);
        addRow(grid, row++, "Type *",        typeCombo);

        // Author + status side-by-side
        HBox authorStatus = new HBox(10, authorField, statusCombo);
        HBox.setHgrow(authorField, Priority.ALWAYS);
        authorField.setMaxWidth(Double.MAX_VALUE);
        addRow(grid, row++, "Author", authorStatus);

        addRow(grid, row++, "Description",   descriptionField);
        addRow(grid, row++, "URL",           urlField);

        // Rating + date side-by-side
        HBox ratingDate = new HBox(10, ratingCombo, dateConsumedPicker);
        HBox.setHgrow(ratingCombo, Priority.ALWAYS);
        HBox.setHgrow(dateConsumedPicker, Priority.ALWAYS);
        addRow(grid, row++, "Rating / Consumed", ratingDate);

        addRow(grid, row++, "Tags",          tagsField);
        addRow(grid, row++, "Notes",         notesField);

        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 12px; -fx-wrap-text: true;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox root = new VBox(8, grid, errorLabel);
        root.setPadding(new Insets(10, 14, 6, 14));
        root.setPrefWidth(560);

        pane.setContent(root);
    }

    private void applyStylesheet(DialogPane pane) {
        var appCss = getClass().getResource("/css/app.css");
        if (appCss != null) pane.getStylesheets().add(appCss.toExternalForm());

        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var themeCss = getClass().getResource(theme);
        if (themeCss != null) pane.getStylesheets().add(themeCss.toExternalForm());
    }

    private void addRow(GridPane grid, int row, String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("auth-field-label");
        grid.add(l, 0, row);
        grid.add(field, 1, row);
    }

    // ------------------------------------------------------------- populate / build

    private void populateFields(Source s) {
        titleField.setText(s.getTitle());
        typeCombo.setValue(s.getSourceType());
        authorField.setText(s.getAuthorCreator() == null ? "" : s.getAuthorCreator());
        descriptionField.setText(s.getDescription() == null ? "" : s.getDescription());
        urlField.setText(s.getUrl() == null ? "" : s.getUrl());
        statusCombo.setValue(s.getStatus());
        if (s.getRating() != null) ratingCombo.setValue(s.getRating());
        if (s.getDateConsumed() != null) dateConsumedPicker.setValue(s.getDateConsumed());
        tagsField.setText(s.getTagString());
        notesField.setText(s.getNotes() == null ? "" : s.getNotes());
    }

    private Source buildSourceFromFields() {
        Source s = (editing == null) ? new Source() : editing;
        s.setTitle(titleField.getText().trim());
        s.setSourceType(typeCombo.getValue());
        s.setAuthorCreator(emptyToNull(authorField.getText()));
        s.setDescription(emptyToNull(descriptionField.getText()));
        s.setUrl(emptyToNull(urlField.getText()));
        s.setStatus(statusCombo.getValue());
        s.setRating(ratingCombo.getValue());
        s.setDateConsumed(dateConsumedPicker.getValue());
        s.setNotes(emptyToNull(notesField.getText()));
        return s;
    }

    /** Extracts the tag names from the comma-separated field. */
    public List<String> getTagNames() {
        String raw = tagsField.getText();
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    // ------------------------------------------------------------- validation

    /** Client-side validation only — service validates again server-side. */
    private String clientValidate() {
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            return "Title is required.";
        }
        if (typeCombo.getValue() == null) {
            return "Please choose a source type.";
        }
        if (dateConsumedPicker.getValue() != null
                && dateConsumedPicker.getValue().isAfter(LocalDate.now().plusDays(1))) {
            return "Date consumed cannot be in the future.";
        }
        return null;
    }

    private void showError(String message) {
        if (message == null || message.isBlank()) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText("⚠  " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}