package com.mindmap.view.flashcard;

import com.mindmap.model.Flashcard;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.util.AppContext;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Modal dialog for creating or editing a flashcard.
 */
public class FlashcardFormDialog {

    private final Dialog<Flashcard> dialog = new Dialog<>();
    private final Flashcard editing;

    private final TextArea questionArea = new TextArea();
    private final TextArea answerArea   = new TextArea();
    private final ComboBox<KnowledgeItem> knowledgeBox = new ComboBox<>();
    private final Label errorLabel = new Label();

    public FlashcardFormDialog(Flashcard editing, List<KnowledgeItem> availableItems) {
        this.editing = editing;
        dialog.setTitle(editing == null ? "New Flashcard" : "Edit Flashcard");
        dialog.setResizable(true);

        buildUI(availableItems);
        if (editing != null) populate(editing);

        dialog.setResultConverter(bt -> {
            if (bt == null || bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            return buildFromFields();
        });

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.addEventFilter(ActionEvent.ACTION, e -> {
                String err = validate();
                if (err != null) {
                    showError(err);
                    e.consume();
                }
            });
        }
    }

    public Optional<Flashcard> showAndWait() {
        return dialog.showAndWait();
    }

    // ------------------------------------------------------------- UI

    private void buildUI(List<KnowledgeItem> availableItems) {
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyStylesheet(pane);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText(editing == null ? "Create" : "Save");
        okBtn.getStyleClass().add("primary-button");

        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Cancel");
        cancelBtn.getStyleClass().add("secondary-button");

        questionArea.setPromptText("Question — e.g. What is time dilation?");
        questionArea.setPrefRowCount(3);
        questionArea.setWrapText(true);
        questionArea.getStyleClass().add("auth-text-field");

        answerArea.setPromptText("Answer — the correct, concise response");
        answerArea.setPrefRowCount(4);
        answerArea.setWrapText(true);
        answerArea.getStyleClass().add("auth-text-field");

        knowledgeBox.setMaxWidth(Double.MAX_VALUE);
        knowledgeBox.setButtonCell(itemCell());
        knowledgeBox.setCellFactory(lv -> itemCell());
        knowledgeBox.setPromptText("(Optional) Link to a knowledge item");

        // Add "none" option
        knowledgeBox.getItems().add(null);
        knowledgeBox.getItems().addAll(availableItems);

        // Labels
        Label ql = new Label("Question *");
        ql.getStyleClass().add("auth-field-label");
        Label al = new Label("Answer *");
        al.getStyleClass().add("auth-field-label");
        Label kl = new Label("Related knowledge item");
        kl.getStyleClass().add("auth-field-label");

        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 12px; -fx-wrap-text: true;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox root = new VBox(10, ql, questionArea, al, answerArea, kl, knowledgeBox, errorLabel);
        root.setPadding(new Insets(12, 14, 8, 14));
        root.setPrefWidth(560);

        pane.setContent(root);
    }

    private ListCell<KnowledgeItem> itemCell() {
        return new ListCell<>() {
            @Override protected void updateItem(KnowledgeItem k, boolean empty) {
                super.updateItem(k, empty);
                if (empty) { setText(null); return; }
                if (k == null) { setText("(No link)"); return; }
                setText(k.getItemType().getIcon() + "  " + k.getTitle());
            }
        };
    }

    private void applyStylesheet(DialogPane pane) {
        var css = getClass().getResource("/css/app.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());

        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var themeCss = getClass().getResource(theme);
        if (themeCss != null) pane.getStylesheets().add(themeCss.toExternalForm());
    }

    private void populate(Flashcard f) {
        questionArea.setText(f.getQuestion());
        answerArea.setText(f.getAnswer());
        if (f.getKnowledgeItemId() != null) {
            for (KnowledgeItem k : knowledgeBox.getItems()) {
                if (k != null && k.getId() == f.getKnowledgeItemId()) {
                    knowledgeBox.setValue(k);
                    break;
                }
            }
        }
    }

    private Flashcard buildFromFields() {
        Flashcard f = (editing == null) ? new Flashcard() : editing;
        f.setQuestion(questionArea.getText().trim());
        f.setAnswer(answerArea.getText().trim());
        KnowledgeItem sel = knowledgeBox.getValue();
        f.setKnowledgeItemId(sel == null ? null : sel.getId());
        return f;
    }

    private String validate() {
        if (questionArea.getText() == null || questionArea.getText().isBlank()) {
            return "Question is required.";
        }
        if (answerArea.getText() == null || answerArea.getText().isBlank()) {
            return "Answer is required.";
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
}