package com.mindmap.view.review;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Review;
import com.mindmap.model.StudySession;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Runs a review session. Shows one {@link ReviewItemCard} at a time,
 * advances on rating, and finishes with a summary screen.
 */
public class ReviewSessionView extends VBox {

    private static final Logger log = LoggerFactory.getLogger(ReviewSessionView.class);

    private final List<Review>   queue;
    private final Runnable       onExit;
    private final Consumer<Integer> onFinished;   // passes the number reviewed

    private final Label      progressLabel = new Label();
    private final ProgressBar progressBar  = new ProgressBar(0);
    private final StackPane  cardHolder    = new StackPane();

    private int index = 0;
    private int correctCount = 0;
    private StudySession session;

    public ReviewSessionView(List<Review> queue,
                             Runnable onExit,
                             Consumer<Integer> onFinished) {
        this.queue = queue;
        this.onExit = onExit;
        this.onFinished = onFinished;

        setSpacing(14);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(20, 40, 20, 40));

        getChildren().addAll(buildTopBar(), buildProgress(), cardHolder);

        startSession();
        showCurrentCard();
    }

    // ------------------------------------------------------------- top bar

    private HBox buildTopBar() {
        Button exitBtn = new Button("✕  End session");
        exitBtn.getStyleClass().add("secondary-button");
        exitBtn.setOnAction(e -> {
            if (Dialogs.confirm("End review session?",
                    "Your progress on completed items is saved. This session will end.")) {
                finishSession();
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
        progressBar.setStyle("-fx-accent: -fx-accent;");

        VBox box = new VBox(4, progressLabel, progressBar);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ------------------------------------------------------------- session

    private void startSession() {
        try {
            session = ServiceRegistry.reviewService().startSession();
        } catch (ServiceException e) {
            log.warn("Could not start session: {}", e.getMessage());
        }
    }

    private void showCurrentCard() {
        if (index >= queue.size()) {
            showSummary();
            return;
        }

        Review review = queue.get(index);
        progressLabel.setText("Card " + (index + 1) + " of " + queue.size());
        progressBar.setProgress((double) index / queue.size());

        ReviewItemCard card = new ReviewItemCard(review, this::handleRating);
        cardHolder.getChildren().setAll(card);
    }

    private void handleRating(int quality) {
        if (index >= queue.size()) return;

        Review review = queue.get(index);

        try {
            ServiceRegistry.reviewService().applyResult(review, quality);
        } catch (ServiceException e) {
            Dialogs.error("Could not save result", e.getMessage());
            return;
        }

        if (quality >= 3) correctCount++;
        index++;
        showCurrentCard();
    }

    // ------------------------------------------------------------- summary

    private void showSummary() {
        progressBar.setProgress(1.0);
        progressLabel.setText("Review complete");

        finishSession();

        int total = queue.size();
        double pct = total == 0 ? 1.0 : (double) correctCount / total;

        Label icon = new Label(pct >= 0.8 ? "🎉" : pct >= 0.5 ? "👍" : "💪");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Review complete!");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        Label detail = new Label(correctCount + " / " + total
                + " recalled   ·   " + Math.round(pct * 100) + "%");
        detail.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 14px;");

        String streakMsg;
        try {
            int streak = ServiceRegistry.reviewService().getStreak();
            streakMsg = streak > 0
                    ? "🔥  Streak: " + streak + " day" + (streak == 1 ? "" : "s")
                    : "Start your streak — review tomorrow!";
        } catch (ServiceException e) {
            streakMsg = "";
        }
        Label streakLabel = new Label(streakMsg);
        streakLabel.setStyle("-fx-text-fill: -fx-accent-hover; -fx-font-weight: bold; "
                + "-fx-font-size: 14px;");

        Button doneBtn = new Button("Back to Review");
        doneBtn.getStyleClass().add("primary-button");
        doneBtn.setPrefWidth(220);
        doneBtn.setPrefHeight(44);
        doneBtn.setOnAction(e -> {
            if (onFinished != null) onFinished.accept(total);
            onExit.run();
        });

        VBox card = new VBox(14, icon, title, detail, streakLabel, doneBtn);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(40, 60, 40, 60));
        card.setMaxWidth(500);

        cardHolder.getChildren().setAll(card);
    }

    private void finishSession() {
        if (session == null) return;
        try {
            ServiceRegistry.reviewService().finishSession(session.getId(), index, correctCount);
        } catch (ServiceException e) {
            log.warn("Could not finish session: {}", e.getMessage());
        }
    }
}