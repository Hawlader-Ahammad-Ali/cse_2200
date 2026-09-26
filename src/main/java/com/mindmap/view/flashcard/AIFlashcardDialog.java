package com.mindmap.view.flashcard;

import com.mindmap.ai.dto.SuggestedFlashcard;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Flashcard;
import com.mindmap.model.KnowledgeItem;
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
 * Dialog for reviewing AI-generated flashcards before saving.
 */
public class AIFlashcardDialog {

    private final Dialog<List<Flashcard>> dialog = new Dialog<>();
    private final KnowledgeItem sourceItem;
    private final List<SuggestedFlashcard> suggestions;
    private final VBox cardsBox = new VBox(10);
    private final Label statusLabel = new Label();

    public AIFlashcardDialog(KnowledgeItem sourceItem, List<SuggestedFlashcard> suggestions) {
        this.sourceItem = sourceItem;
        this.suggestions = suggestions;
        dialog.setTitle("AI Generated Flashcards");
        dialog.setResizable(true);
        buildUI();
    }

    public Optional<List<Flashcard>> showAndWait() {
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

        Label title = new Label("🤖  AI Generated Flashcards");
        title.getStyleClass().add("view-header");

        Label sub = new Label("For: " + sourceItem.getTitle());
        sub.getStyleClass().add("view-subtitle");

        Label hint = new Label("Review each card below. Uncheck any you don't want. "
                + "You can edit the question and answer inline.");
        hint.setWrapText(true);
        hint.getStyleClass().add("placeholder-desc");
        hint.setPadding(new Insets(4, 0, 6, 0));

        for (SuggestedFlashcard s : suggestions) {
            cardsBox.getChildren().add(buildCardRow(s));
        }

        ScrollPane scroll = new ScrollPane(cardsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background-color: transparent;");

        statusLabel.getStyleClass().add("view-subtitle");
        updateStatus();

        VBox root = new VBox(6, title, sub, hint, scroll, statusLabel);
        root.setPadding(new Insets(10, 14, 8, 14));
        root.setPrefWidth(640);
        pane.setContent(root);

        dialog.setResultConverter(bt -> {
            if (bt == null || bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;

            List<SuggestedFlashcard> accepted = new ArrayList<>();
            for (SuggestedFlashcard s : suggestions) {
                if (s.isAccepted()) accepted.add(s);
            }
            if (accepted.isEmpty()) {
                Dialogs.warning("Nothing selected", "Accept at least one card, or cancel.");
                return null;
            }
            try {
                return ServiceRegistry.flashcardService()
                        .createFromSuggestions(accepted, sourceItem.getId());
            } catch (ServiceException e) {
                Dialogs.error("Could not save flashcards", e.getMessage());
                return null;
            }
        });
    }

    private void applyStylesheet(DialogPane pane) {
        var css = getClass().getResource("/css/app.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());

        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var themeCss = getClass().getResource(theme);
        if (themeCss != null) pane.getStylesheets().add(themeCss.toExternalForm());
    }

    private VBox buildCardRow(SuggestedFlashcard s) {
        CheckBox acceptBox = new CheckBox();
        acceptBox.setSelected(true);
        acceptBox.selectedProperty().addListener((o, ov, nv) -> {
            s.setAccepted(nv);
            updateStatus();
        });

        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size: 16px;");

        Label header = new Label("AI generated");
        header.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox topRow = new HBox(8, acceptBox, icon, header);
        topRow.setAlignment(Pos.CENTER_LEFT);

        TextArea questionArea = new TextArea(s.getQuestion());
        questionArea.getStyleClass().add("auth-text-field");
        questionArea.setPromptText("Question");
        questionArea.setPrefRowCount(2);
        questionArea.setWrapText(true);
        questionArea.textProperty().addListener((o, ov, nv) -> s.setQuestion(nv));

        TextArea answerArea = new TextArea(s.getAnswer());
        answerArea.getStyleClass().add("auth-text-field");
        answerArea.setPromptText("Answer");
        answerArea.setPrefRowCount(3);
        answerArea.setWrapText(true);
        answerArea.textProperty().addListener((o, ov, nv) -> s.setAnswer(nv));

        Label qLabel = new Label("Q");
        qLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px; -fx-min-width: 16;");
        Label aLabel = new Label("A");
        aLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px; -fx-min-width: 16;");

        HBox qRow = new HBox(6, qLabel, questionArea);
        HBox.setHgrow(questionArea, Priority.ALWAYS);
        HBox aRow = new HBox(6, aLabel, answerArea);
        HBox.setHgrow(answerArea, Priority.ALWAYS);

        VBox card = new VBox(6, topRow, qRow, aRow);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8;");

        acceptBox.selectedProperty().addListener((o, ov, nv) -> card.setOpacity(nv ? 1.0 : 0.5));

        return card;
    }

    private void updateStatus() {
        long n = suggestions.stream().filter(SuggestedFlashcard::isAccepted).count();
        statusLabel.setText(n + " of " + suggestions.size() + " selected");
    }
}