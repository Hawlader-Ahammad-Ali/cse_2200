package com.mindmap.view.quiz;

import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.QuizQuestion;
import com.mindmap.model.QuizQuestionType;
import com.mindmap.util.AppContext;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manual question editor. Handles MCQ, True/False, and Short answer
 * with a dynamically changing form.
 */
public class QuizQuestionFormDialog {

    private final Dialog<QuizQuestion> dialog = new Dialog<>();
    private final QuizQuestion editing;

    private final ComboBox<QuizQuestionType> typeBox = new ComboBox<>();
    private final TextArea questionArea = new TextArea();
    private final TextArea explanationArea = new TextArea();
    private final ComboBox<KnowledgeItem> knowledgeBox = new ComboBox<>();
    private final Label errorLabel = new Label();

    // Container for the answer fields (changes by type)
    private final VBox answerFieldsBox = new VBox(8);

    // MCQ
    private final List<TextField> mcqOptions = new ArrayList<>();
    private final ComboBox<String> mcqCorrect = new ComboBox<>();

    // True/False
    private final ComboBox<String> tfCorrect = new ComboBox<>();

    // Short
    private final TextField shortAnswer = new TextField();

    public QuizQuestionFormDialog(QuizQuestion editing, List<KnowledgeItem> availableItems) {
        this.editing = editing;
        dialog.setTitle(editing == null ? "New Question" : "Edit Question");
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
                if (err != null) { showError(err); e.consume(); }
            });
        }
    }

    public Optional<QuizQuestion> showAndWait() { return dialog.showAndWait(); }

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

        typeBox.getItems().addAll(QuizQuestionType.values());
        typeBox.setValue(QuizQuestionType.MCQ);
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setOnAction(e -> rebuildAnswerFields());

        questionArea.setPromptText("The question text");
        questionArea.setPrefRowCount(2);
        questionArea.setWrapText(true);
        questionArea.getStyleClass().add("auth-text-field");

        explanationArea.setPromptText("(Optional) explanation shown after answering");
        explanationArea.setPrefRowCount(2);
        explanationArea.setWrapText(true);
        explanationArea.getStyleClass().add("auth-text-field");

        knowledgeBox.setMaxWidth(Double.MAX_VALUE);
        knowledgeBox.setButtonCell(itemCell());
        knowledgeBox.setCellFactory(lv -> itemCell());
        knowledgeBox.getItems().add(null);
        knowledgeBox.getItems().addAll(availableItems);

        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox root = new VBox(10,
                label("Type"), typeBox,
                label("Question *"), questionArea,
                label("Answer fields"), answerFieldsBox,
                label("Explanation"), explanationArea,
                label("Related knowledge item"), knowledgeBox,
                errorLabel);
        root.setPadding(new Insets(12, 14, 8, 14));
        root.setPrefWidth(560);
        pane.setContent(root);

        rebuildAnswerFields();
    }

    private Label label(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("auth-field-label");
        return l;
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

    private void rebuildAnswerFields() {
        answerFieldsBox.getChildren().clear();
        QuizQuestionType type = typeBox.getValue();
        if (type == null) return;

        switch (type) {
            case MCQ -> {
                mcqOptions.clear();
                mcqCorrect.getItems().clear();
                VBox optionsBox = new VBox(6);
                for (int i = 0; i < 4; i++) {
                    TextField opt = new TextField();
                    opt.setPromptText("Option " + (char)('A' + i));
                    opt.getStyleClass().add("auth-text-field");
                    opt.textProperty().addListener((o, ov, nv) -> refreshCorrectChoices());
                    mcqOptions.add(opt);
                    optionsBox.getChildren().add(opt);
                }
                mcqCorrect.setPromptText("Pick the correct option");
                mcqCorrect.setMaxWidth(Double.MAX_VALUE);
                answerFieldsBox.getChildren().addAll(optionsBox, label("Correct option"), mcqCorrect);
            }
            case TRUE_FALSE -> {
                tfCorrect.getItems().setAll("true", "false");
                tfCorrect.setValue("true");
                tfCorrect.setMaxWidth(Double.MAX_VALUE);
                answerFieldsBox.getChildren().addAll(label("Correct answer"), tfCorrect);
            }
            case SHORT -> {
                shortAnswer.setPromptText("Expected answer (1-3 words)");
                shortAnswer.getStyleClass().add("auth-text-field");
                answerFieldsBox.getChildren().addAll(label("Expected answer"), shortAnswer);
            }
        }
    }

    private void refreshCorrectChoices() {
        String prev = mcqCorrect.getValue();
        mcqCorrect.getItems().clear();
        for (TextField tf : mcqOptions) {
            String v = tf.getText();
            if (v != null && !v.isBlank()) mcqCorrect.getItems().add(v);
        }
        if (prev != null && mcqCorrect.getItems().contains(prev)) mcqCorrect.setValue(prev);
    }

    private void applyStylesheet(DialogPane pane) {
        var css = getClass().getResource("/css/app.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());
        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var tcss = getClass().getResource(theme);
        if (tcss != null) pane.getStylesheets().add(tcss.toExternalForm());
    }

    private void populate(QuizQuestion q) {
        typeBox.setValue(q.getQuestionType());
        questionArea.setText(q.getQuestionText());
        explanationArea.setText(q.getExplanation() == null ? "" : q.getExplanation());
        if (q.getKnowledgeItemId() != null) {
            for (KnowledgeItem k : knowledgeBox.getItems()) {
                if (k != null && k.getId() == q.getKnowledgeItemId()) {
                    knowledgeBox.setValue(k);
                    break;
                }
            }
        }
        // Rebuild fields before populating
        rebuildAnswerFields();

        switch (q.getQuestionType()) {
            case MCQ -> {
                for (int i = 0; i < q.getOptions().size() && i < mcqOptions.size(); i++) {
                    mcqOptions.get(i).setText(q.getOptions().get(i));
                }
                refreshCorrectChoices();
                mcqCorrect.setValue(q.getCorrectAnswer());
            }
            case TRUE_FALSE -> tfCorrect.setValue(q.getCorrectAnswer());
            case SHORT -> shortAnswer.setText(q.getCorrectAnswer());
        }
    }

    private QuizQuestion buildFromFields() {
        QuizQuestion q = (editing == null) ? new QuizQuestion() : editing;
        q.setQuestionType(typeBox.getValue());
        q.setQuestionText(questionArea.getText().trim());
        q.setExplanation(emptyToNull(explanationArea.getText()));
        KnowledgeItem sel = knowledgeBox.getValue();
        q.setKnowledgeItemId(sel == null ? null : sel.getId());

        switch (q.getQuestionType()) {
            case MCQ -> {
                List<String> opts = new ArrayList<>();
                for (TextField tf : mcqOptions) {
                    String v = tf.getText();
                    if (v != null && !v.isBlank()) opts.add(v.trim());
                }
                q.setOptions(opts);
                q.setCorrectAnswer(mcqCorrect.getValue());
            }
            case TRUE_FALSE -> {
                q.setOptions(new ArrayList<>());
                q.setCorrectAnswer(tfCorrect.getValue());
            }
            case SHORT -> {
                q.setOptions(new ArrayList<>());
                q.setCorrectAnswer(shortAnswer.getText().trim());
            }
        }
        return q;
    }

    private String validate() {
        if (questionArea.getText() == null || questionArea.getText().isBlank())
            return "Question text is required.";
        QuizQuestionType type = typeBox.getValue();
        if (type == null) return "Type is required.";

        switch (type) {
            case MCQ -> {
                long filled = mcqOptions.stream()
                        .filter(t -> t.getText() != null && !t.getText().isBlank()).count();
                if (filled < 2) return "Multiple choice needs at least 2 options.";
                if (mcqCorrect.getValue() == null) return "Pick the correct option.";
            }
            case TRUE_FALSE -> {
                if (tfCorrect.getValue() == null) return "Pick true or false.";
            }
            case SHORT -> {
                if (shortAnswer.getText() == null || shortAnswer.getText().isBlank())
                    return "Expected answer is required.";
            }
        }
        return null;
    }

    private void showError(String message) {
        if (message == null) {
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