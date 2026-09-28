package com.mindmap.view.quiz;

import com.mindmap.model.QuizQuestion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Results screen after a quiz session.
 */
public class QuizResultsView extends VBox {

    private final List<QuizSessionView.Result> results;
    private final Runnable onExit;

    public QuizResultsView(List<QuizSessionView.Result> results, Runnable onExit) {
        this.results = results;
        this.onExit = onExit;

        setSpacing(14);
        setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(buildHeader(), buildScroll());
    }

    private VBox buildHeader() {
        int total = results.size();
        int correct = (int) results.stream().filter(QuizSessionView.Result::correct).count();
        double pct = total == 0 ? 0 : (double) correct / total;

        Label icon = new Label(pct >= 0.8 ? "🎉" : pct >= 0.5 ? "👍" : "💪");
        icon.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Quiz complete!");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        Label score = new Label(correct + " / " + total
                + "   ·   " + Math.round(pct * 100) + "%");
        score.setStyle("-fx-font-size: 18px; -fx-text-fill: -fx-text-secondary;");

        VBox card = new VBox(10, icon, title, score);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(30, 40, 30, 40));

        return card;
    }

    private ScrollPane buildScroll() {
        VBox body = new VBox(10);
        body.setPadding(new Insets(4, 2, 4, 2));

        Label header = new Label("Review");
        header.getStyleClass().add("section-header");
        body.getChildren().add(header);

        for (int i = 0; i < results.size(); i++) {
            body.getChildren().add(buildResultRow(i + 1, results.get(i)));
        }

        // Weak-topic message
        List<QuizSessionView.Result> wrong =
                results.stream().filter(r -> !r.correct()).toList();
        if (!wrong.isEmpty()) {
            body.getChildren().add(new Separator());
            Label tip = new Label("Tip: these questions will appear more often "
                    + "when you choose \"Prioritize weak questions\" next time.");
            tip.setWrapText(true);
            tip.getStyleClass().add("placeholder-desc");
            body.getChildren().add(tip);
        }

        Button doneBtn = new Button("←  Back to Quiz");
        doneBtn.getStyleClass().add("primary-button");
        doneBtn.setPrefHeight(42);
        doneBtn.setOnAction(e -> onExit.run());

        HBox btnRow = new HBox(doneBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(12, 0, 4, 0));
        body.getChildren().add(btnRow);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private HBox buildResultRow(int num, QuizSessionView.Result result) {
        QuizQuestion q = result.question();
        boolean ok = result.correct();

        Label badge = new Label(ok ? "✅" : "❌");
        badge.setStyle("-fx-font-size: 16px; -fx-min-width: 22;");

        Label numLabel = new Label(String.valueOf(num));
        numLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px; -fx-min-width: 20;");

        Label qText = new Label(q.getQuestionText());
        qText.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 12.5px;");
        qText.setWrapText(true);
        HBox.setHgrow(qText, Priority.ALWAYS);

        Label typeLabel = new Label(q.getQuestionType().getLabel());
        typeLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px;");

        HBox row = new HBox(10, badge, numLabel, qText, typeLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8;");

        return row;
    }
}