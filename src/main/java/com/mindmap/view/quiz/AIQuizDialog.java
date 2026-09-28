package com.mindmap.view.quiz;

import com.mindmap.ai.dto.SuggestedQuizQuestion;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.QuizQuestion;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
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
 * Review AI-generated quiz questions before saving them.
 */
public class AIQuizDialog {

    private final Dialog<List<QuizQuestion>> dialog = new Dialog<>();
    private final KnowledgeItem sourceItem;
    private final List<SuggestedQuizQuestion> suggestions;
    private final VBox cardsBox = new VBox(10);
    private final Label statusLabel = new Label();

    public AIQuizDialog(KnowledgeItem sourceItem, List<SuggestedQuizQuestion> suggestions) {
        this.sourceItem = sourceItem;
        this.suggestions = suggestions;
        dialog.setTitle("AI Generated Quiz Questions");
        dialog.setResizable(true);
        buildUI();
    }

    public Optional<List<QuizQuestion>> showAndWait() {
        dialog.showAndWait();
        return Optional.ofNullable(dialog.getResult());
    }

    private void buildUI() {
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyStylesheet(pane);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Create Selected");
        okBtn.getStyleClass().add("primary-button");
        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Cancel");
        cancelBtn.getStyleClass().add("secondary-button");

        Label title = new Label("🤖  AI Generated Questions");
        title.getStyleClass().add("view-header");
        Label sub = new Label("For: " + sourceItem.getTitle());
        sub.getStyleClass().add("view-subtitle");
        Label hint = new Label("Review each question. Uncheck any you don't want, and edit inline if needed.");
        hint.setWrapText(true);
        hint.getStyleClass().add("placeholder-desc");
        hint.setPadding(new Insets(4, 0, 6, 0));

        for (SuggestedQuizQuestion s : suggestions) {
            cardsBox.getChildren().add(buildCard(s));
        }

        ScrollPane scroll = new ScrollPane(cardsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(460);
        scroll.setStyle("-fx-background-color: transparent;");

        statusLabel.getStyleClass().add("view-subtitle");
        updateStatus();

        VBox root = new VBox(6, title, sub, hint, scroll, statusLabel);
        root.setPadding(new Insets(10, 14, 8, 14));
        root.setPrefWidth(680);
        pane.setContent(root);

        dialog.setResultConverter(bt -> {
            if (bt == null || bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            List<SuggestedQuizQuestion> accepted = new ArrayList<>();
            for (SuggestedQuizQuestion s : suggestions) if (s.isAccepted()) accepted.add(s);
            if (accepted.isEmpty()) {
                Dialogs.warning("Nothing selected", "Accept at least one question.");
                return null;
            }
            try {
                return ServiceRegistry.quizService()
                        .createFromSuggestions(accepted, sourceItem.getId());
            } catch (ServiceException e) {
                Dialogs.error("Could not save questions", e.getMessage());
                return null;
            }
        });
    }

    private VBox buildCard(SuggestedQuizQuestion s) {
        CheckBox acceptBox = new CheckBox();
        acceptBox.setSelected(true);
        acceptBox.selectedProperty().addListener((o, ov, nv) -> {
            s.setAccepted(nv);
            updateStatus();
        });

        Label typeLabel = new Label(s.getType() == null ? "MCQ" : s.getType());
        typeLabel.setStyle("-fx-background-color: -fx-accent-soft; -fx-text-fill: -fx-accent-hover; "
                + "-fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");

        HBox topRow = new HBox(8, acceptBox, typeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        TextArea questionArea = new TextArea(s.getQuestion());
        questionArea.getStyleClass().add("auth-text-field");
        questionArea.setPrefRowCount(2);
        questionArea.setWrapText(true);
        questionArea.textProperty().addListener((o, ov, nv) -> s.setQuestion(nv));

        TextArea answerArea = new TextArea(s.getCorrect());
        answerArea.getStyleClass().add("auth-text-field");
        answerArea.setPrefRowCount(1);
        answerArea.setWrapText(true);
        answerArea.setPromptText("Correct answer");
        answerArea.textProperty().addListener((o, ov, nv) -> s.setCorrect(nv));

        // Options (MCQ only)
        VBox optionsBox = new VBox(2);
        if (s.getOptions() != null && !s.getOptions().isEmpty()) {
            for (int i = 0; i < s.getOptions().size(); i++) {
                Label opt = new Label("   " + (char)('A' + i) + ". " + s.getOptions().get(i));
                opt.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");
                optionsBox.getChildren().add(opt);
            }
        }

        VBox card = new VBox(6, topRow, questionArea, optionsBox, answerArea);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8;");

        acceptBox.selectedProperty().addListener((o, ov, nv) -> card.setOpacity(nv ? 1.0 : 0.5));

        return card;
    }

    private void updateStatus() {
        long n = suggestions.stream().filter(SuggestedQuizQuestion::isAccepted).count();
        statusLabel.setText(n + " of " + suggestions.size() + " selected");
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