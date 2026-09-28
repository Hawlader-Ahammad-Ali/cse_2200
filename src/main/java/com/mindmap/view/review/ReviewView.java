package com.mindmap.view.review;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Review;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Review landing page. Shows stats + Start Review button, and swaps to
 * {@link ReviewSessionView} when the user starts.
 */
public class ReviewView extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(ReviewView.class);

    private final VBox landing = new VBox(14);

    public ReviewView() {
        buildLanding();
        getChildren().add(landing);
    }

    // ------------------------------------------------------------- landing

    private void buildLanding() {
        landing.setPadding(new Insets(4, 0, 0, 0));

        Label title = new Label("Review");
        title.getStyleClass().add("view-header");

        Label subtitle = new Label("Spaced repetition keeps concepts fresh and helps you remember longer.");
        subtitle.getStyleClass().add("view-subtitle");

        VBox header = new VBox(2, title, subtitle);

        VBox card = buildStartCard();
        VBox stats = buildStatsCard();

        ScrollPane scroll = new ScrollPane(new VBox(14, header, card, stats));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        landing.getChildren().add(scroll);
    }

    private VBox buildStartCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(28, 32, 28, 32));

        int due;
        try { due = ServiceRegistry.reviewService().getDueCount(); }
        catch (ServiceException e) { due = 0; }

        Label icon = new Label(due == 0 ? "✅" : "🔄");
        icon.setStyle("-fx-font-size: 40px;");

        Label big;
        if (due == 0) {
            big = new Label("Nothing to review right now");
            big.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");
        } else {
            big = new Label(due + " item" + (due == 1 ? "" : "s") + " due today");
            big.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");
        }

        Label hint = new Label(due == 0
                ? "Come back tomorrow — your future self will thank you."
                : "Take a few minutes to strengthen your memory.");
        hint.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 13px;");

        Button startBtn = new Button("Start Review");
        startBtn.getStyleClass().add("primary-button");
        startBtn.setPrefWidth(200);
        startBtn.setPrefHeight(44);
        startBtn.setDisable(due == 0);
        startBtn.setOnAction(e -> startSession());

        VBox textBox = new VBox(6, big, hint);
        HBox row = new HBox(20, icon, textBox);
        row.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(row, startBtn);
        return card;
    }

    private VBox buildStatsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 24, 20, 24));

        Label header = new Label("Review statistics");
        header.getStyleClass().add("section-header");

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(12);

        try {
            var svc = ServiceRegistry.reviewService();
            int due       = svc.getDueCount();
            int streak    = svc.getStreak();
            int today     = svc.getReviewedToday();
            int total     = svc.getTotalEnrolled();
            double avgEase= svc.getAverageEase();
            int sessions  = svc.getTotalCompletedSessions();

            grid.add(statBlock("Due today",  String.valueOf(due)),                    0, 0);
            grid.add(statBlock("Reviewed today", String.valueOf(today)),              1, 0);
            grid.add(statBlock("Current streak", "🔥 " + streak + " day" + (streak == 1 ? "" : "s")), 2, 0);
            grid.add(statBlock("Total enrolled", String.valueOf(total)),              0, 1);
            grid.add(statBlock("Average ease", String.format("%.2f", avgEase)),       1, 1);
            grid.add(statBlock("Sessions", String.valueOf(sessions)),                 2, 1);
        } catch (ServiceException e) {
            grid.add(new Label("Could not load stats."), 0, 0);
        }

        card.getChildren().addAll(header, grid);
        return card;
    }

    private VBox statBlock(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 20px; -fx-font-weight: bold;");

        VBox box = new VBox(2, l, v);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ------------------------------------------------------------- session

    private void startSession() {
        List<Review> queue;
        try {
            queue = ServiceRegistry.reviewService().getDueToday();
        } catch (ServiceException e) {
            Dialogs.error("Could not load review queue", e.getMessage());
            return;
        }
        if (queue.isEmpty()) {
            Dialogs.info("Nothing to review", "You're all caught up!");
            return;
        }

        log.info("Starting review session with {} items", queue.size());

        ReviewSessionView session = new ReviewSessionView(
                queue,
                this::returnToLanding,
                this::onSessionFinished
        );
        getChildren().setAll(session);
    }

    private void onSessionFinished(int reviewed) {
        AppContext.getInstance().setStatusMessage("Reviewed " + reviewed + " item(s).");
    }

    private void returnToLanding() {
        // Rebuild landing so stats reflect the session we just finished
        landing.getChildren().clear();
        buildLanding();
        getChildren().setAll(landing);
    }
}