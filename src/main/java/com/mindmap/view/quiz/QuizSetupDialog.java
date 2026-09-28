package com.mindmap.view.quiz;

import com.mindmap.util.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Small dialog: how many questions, include weak-first?
 */
public class QuizSetupDialog {

    public record Setup(int count, boolean weakFirst) { }

    private final Dialog<Setup> dialog = new Dialog<>();
    private final int available;

    public QuizSetupDialog(int availableQuestions) {
        this.available = availableQuestions;
        dialog.setTitle("Start Quiz");
        buildUI();
    }

    public Optional<Setup> showAndWait() { return dialog.showAndWait(); }

    private void buildUI() {
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyStylesheet(pane);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Start");
        okBtn.getStyleClass().add("primary-button");
        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Cancel");
        cancelBtn.getStyleClass().add("secondary-button");

        Label title = new Label("Quiz Setup");
        title.getStyleClass().add("view-header");
        Label sub = new Label(available + " question" + (available == 1 ? "" : "s") + " available");
        sub.getStyleClass().add("view-subtitle");

        Spinner<Integer> countSpinner = new Spinner<>(1, Math.max(1, available), Math.min(10, available));
        countSpinner.setEditable(true);
        countSpinner.setPrefWidth(120);

        CheckBox weakOnly = new CheckBox("Prioritize weak questions");
        weakOnly.getStyleClass().add("takeaway-filter-checkbox");

        HBox countRow = new HBox(10, new Label("Questions:"), countSpinner);
        countRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, title, sub, new Separator(), countRow, weakOnly);
        root.setPadding(new Insets(12, 16, 8, 16));
        root.setPrefWidth(360);
        pane.setContent(root);

        dialog.setResultConverter(bt -> {
            if (bt == null || bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            return new Setup(countSpinner.getValue(), weakOnly.isSelected());
        });
    }

    private void applyStylesheet(DialogPane pane) {
        var css = getClass().getResource("/css/app.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var tcss = getClass().getResource(theme);
        if (tcss != null) pane.getStylesheets().add(tcss.toExternalForm());
    }
}