package com.mindmap.view.flashcard;

import com.mindmap.algorithm.SM2Scheduler;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Flashcard;
import com.mindmap.model.Review;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Flashcard review session. Reuses the review rating flow from Phase 11,
 * but displays flashcards instead of knowledge items.
 */
public class FlashcardReviewSession extends VBox {

    private static final Logger log = LoggerFactory.getLogger(FlashcardReviewSession.class);

    /** Bundle: flashcard + its review row. */
    private record CardPair(Flashcard card, Review review) { }

    private final List<CardPair> queue = new ArrayList<>();
    private final Runnable onExit;
    private final Consumer<Integer> onFinished;

    private final Label       progressLabel = new Label();
    private final ProgressBar progressBar   = new ProgressBar(0);
    private final StackPane   cardHolder    = new StackPane();

    private int index = 0;
    private int correctCount = 0;

    public FlashcardReviewSession(List<Flashcard> cards,
                                  Runnable onExit,
                                  Consumer<Integer> onFinished) {
        this.onExit = onExit;
        this.onFinished = onFinished;

        // Load the review record for each card
        for (Flashcard c : cards) {
            try {
                ServiceRegistry.reviewService().getReviewForFlashcard(c.getId())
                        .ifPresent(r -> queue.add(new CardPair(c, r)));
            } catch (ServiceException e) {
                log.warn("Could not load review for flashcard {}: {}", c.getId(), e.getMessage());
            }
        }

        setSpacing(14);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(20, 40, 20, 40));

        getChildren().addAll(buildTopBar(), buildProgress(), cardHolder);
        showCurrentCard();
    }

    private HBox buildTopBar() {
        Button exitBtn = new Button("✕  End session");
        exitBtn.getStyleClass().add("secondary-button");
        exitBtn.setOnAction(e -> {
            if (Dialogs.confirm("End session?", "Your completed cards are saved.")) {
                onExit.run();
            }
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(exitBtn, spacer);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildProgress() {
        progressLabel.getStyleClass().add("view-subtitle");
        progressBar.setPrefWidth(500);
        VBox box = new VBox(4, progressLabel, progressBar);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void showCurrentCard() {
        if (index >= queue.size()) {
            showSummary();
            return;
        }

        CardPair pair = queue.get(index);
        progressLabel.setText("Card " + (index + 1) + " of " + queue.size());
        progressBar.setProgress((double) index / queue.size());

        VBox card = buildCardView(pair, false);
        cardHolder.getChildren().setAll(card);
    }

    private VBox buildCardView(CardPair pair, boolean revealed) {
        VBox root = new VBox(14);
        root.setAlignment(Pos.TOP_CENTER);

        // Question card
        VBox qCard = new VBox(10);
        qCard.getStyleClass().add("card");
        qCard.setPadding(new Insets(24, 28, 24, 28));
        qCard.setMaxWidth(680);

        Label qTag = new Label("Question");
        qTag.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");

        Label qText = new Label(pair.card().getQuestion());
        qText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; "
                + "-fx-text-fill: -fx-text-primary;");
        qText.setWrapText(true);
        qText.setAlignment(Pos.CENTER);
        qText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        qText.setMaxWidth(620);

        VBox qBody = new VBox(4, qTag, qText);
        qBody.setAlignment(Pos.CENTER);
        qCard.getChildren().add(qBody);

        root.getChildren().add(qCard);

        if (!revealed) {
            Button reveal = new Button("Show Answer");
            reveal.getStyleClass().add("primary-button");
            reveal.setPrefWidth(240);
            reveal.setPrefHeight(44);
            reveal.setOnAction(e -> {
                cardHolder.getChildren().setAll(buildCardView(pair, true));
            });
            root.getChildren().add(new HBox(reveal) {{ setAlignment(Pos.CENTER); }});
            return root;
        }

        // Answer card
        VBox aCard = new VBox(10);
        aCard.getStyleClass().add("card");
        aCard.setPadding(new Insets(20, 24, 20, 24));
        aCard.setMaxWidth(680);

        Label aTag = new Label("Answer");
        aTag.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");

        Label aText = new Label(pair.card().getAnswer());
        aText.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text-primary;");
        aText.setWrapText(true);
        aText.setMaxWidth(620);

        aCard.getChildren().addAll(aTag, aText);

        // Link to knowledge item if present
        if (pair.card().getKnowledgeItemId() != null) {
            Label linked = new Label("📍  Linked to a knowledge item — see details for context");
            linked.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px;");
            aCard.getChildren().add(linked);
        }

        ScrollPane aScroll = new ScrollPane(aCard);
        aScroll.setFitToWidth(true);
        aScroll.setMaxHeight(260);
        aScroll.setStyle("-fx-background-color: transparent;");

        // Rating row
        HBox ratings = new HBox(10,
                ratingButton("Again", "#E74C3C", SM2Scheduler.QUALITY_AGAIN, pair),
                ratingButton("Hard",  "#F39C12", SM2Scheduler.QUALITY_HARD,  pair),
                ratingButton("Good",  "#27AE60", SM2Scheduler.QUALITY_GOOD,  pair),
                ratingButton("Easy",  "#3498DB", SM2Scheduler.QUALITY_EASY,  pair));
        ratings.setAlignment(Pos.CENTER);

        root.getChildren().addAll(aScroll, ratings);
        return root;
    }

    private Button ratingButton(String text, String color, int quality, CardPair pair) {
        Button b = new Button(text);
        b.setPrefWidth(110);
        b.setPrefHeight(44);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; "
                + "-fx-font-size: 13px;");
        b.setOnAction(e -> handleRating(pair, quality));
        return b;
    }

    private void handleRating(CardPair pair, int quality) {
        try {
            ServiceRegistry.reviewService().applyResult(pair.review(), quality);
        } catch (ServiceException e) {
            Dialogs.error("Could not save result", e.getMessage());
            return;
        }
        if (quality >= 3) correctCount++;
        index++;
        showCurrentCard();
    }

    private void showSummary() {
        progressBar.setProgress(1.0);
        progressLabel.setText("Session complete");

        int total = queue.size();
        double pct = total == 0 ? 1.0 : (double) correctCount / total;

        Label icon = new Label(pct >= 0.8 ? "🎉" : pct >= 0.5 ? "👍" : "💪");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Session complete!");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        Label detail = new Label(correctCount + " / " + total
                + " recalled   ·   " + Math.round(pct * 100) + "%");
        detail.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 14px;");

        Button doneBtn = new Button("Back to Flashcards");
        doneBtn.getStyleClass().add("primary-button");
        doneBtn.setPrefWidth(220);
        doneBtn.setPrefHeight(44);
        doneBtn.setOnAction(e -> {
            if (onFinished != null) onFinished.accept(total);
            onExit.run();
        });

        VBox card = new VBox(14, icon, title, detail, doneBtn);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(40, 60, 40, 60));
        card.setMaxWidth(500);

        cardHolder.getChildren().setAll(card);
    }
}