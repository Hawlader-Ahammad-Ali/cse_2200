package com.mindmap.view.review;

import com.mindmap.algorithm.SM2Scheduler;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Review;
import com.mindmap.model.Source;
import com.mindmap.util.exceptions.ServiceException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Single review card. Starts hidden ("Show Answer" prompt),
 * then reveals the takeaway + metadata + rating buttons.
 */
public class ReviewItemCard extends VBox {

    private final Review review;
    private final IntConsumer onRated;

    private final VBox answerBox = new VBox(10);
    private boolean revealed = false;

    public ReviewItemCard(Review review, IntConsumer onRated) {
        this.review = review;
        this.onRated = onRated;

        setSpacing(14);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 10, 0));

        getChildren().addAll(
                buildQuestionCard(),
                buildAnswerBox()
        );
    }

    // ------------------------------------------------------------- question

    private VBox buildQuestionCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 24, 28));

        KnowledgeItem k = review.getKnowledgeItem();
        if (k == null) {
            Label err = new Label("(Item missing)");
            card.getChildren().add(err);
            return card;
        }

        Label typeIcon = new Label(k.getItemType().getIcon());
        typeIcon.setStyle("-fx-font-size: 40px;");

        Label title = new Label(k.getTitle());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");
        title.setWrapText(true);
        title.setMaxWidth(640);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label hint = new Label("Try to recall what this concept means. Then reveal the answer.");
        hint.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 12px;");
        hint.setWrapText(true);

        VBox header = new VBox(8, typeIcon, title, hint);
        header.setAlignment(Pos.CENTER);

        card.getChildren().add(header);
        return card;
    }

    // ------------------------------------------------------------- answer + rating

    private VBox buildAnswerBox() {
        // Reveal button
        Button revealBtn = new Button("Show Answer");
        revealBtn.getStyleClass().add("primary-button");
        revealBtn.setPrefWidth(240);
        revealBtn.setPrefHeight(44);
        revealBtn.setOnAction(e -> reveal());

        HBox revealRow = new HBox(revealBtn);
        revealRow.setAlignment(Pos.CENTER);

        // Actual answer content
        answerBox.getChildren().add(buildAnswerContent());
        answerBox.setVisible(false);
        answerBox.setManaged(false);

        // Rating buttons at the bottom
        HBox ratingRow = buildRatingRow();
        ratingRow.setVisible(false);
        ratingRow.setManaged(false);

        // Keep references so we can show/hide together
        answerBox.getProperties().put("revealRow", revealRow);
        answerBox.getProperties().put("ratingRow", ratingRow);

        VBox wrapper = new VBox(14, revealRow, answerBox, ratingRow);
        wrapper.setAlignment(Pos.TOP_CENTER);
        return wrapper;
    }

    private VBox buildAnswerContent() {
        KnowledgeItem k = review.getKnowledgeItem();

        VBox content = new VBox(10);
        content.getStyleClass().add("card");
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setMaxWidth(680);

        if (k == null) return content;

        // Description
        if (k.getDescription() != null && !k.getDescription().isBlank()) {
            Label desc = new Label(k.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 14px;");
            content.getChildren().add(desc);
        }

        // Takeaway
        if (k.getPersonalNote() != null && !k.getPersonalNote().isBlank()) {
            content.getChildren().add(new Separator());
            Label h = new Label("📝  My Takeaway");
            h.getStyleClass().add("section-header");

            Label note = new Label(k.getPersonalNote());
            note.setWrapText(true);
            note.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 13px; "
                    + "-fx-font-style: italic;");
            content.getChildren().addAll(h, note);
        }

        // Meta row
        content.getChildren().add(new Separator());
        HBox meta = new HBox(16,
                metaChip("Category", k.getCategory() == null ? "—" : k.getCategory()),
                metaChip("Difficulty", k.getDifficulty()),
                metaChip("Importance", k.getImportance()));
        meta.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(meta);

        // Sources ("Where did I learn this?")
        try {
            List<Source> sources = ServiceRegistry.knowledgeService().getSourcesFor(k.getId());
            if (!sources.isEmpty()) {
                content.getChildren().add(new Separator());
                Label h = new Label("📍  Where I Learned This");
                h.getStyleClass().add("section-header");
                content.getChildren().add(h);

                for (Source s : sources) {
                    HBox r = new HBox(8);
                    Label sIcon = new Label(s.getSourceType().getIcon());
                    Label sTitle = new Label(s.getTitle());
                    sTitle.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12.5px;");
                    r.getChildren().addAll(sIcon, sTitle);
                    content.getChildren().add(r);
                }
            }
        } catch (ServiceException e) {
            // ignore
        }

        // Review metadata (interval, ease)
        content.getChildren().add(new Separator());
        HBox meta2 = new HBox(16,
                metaChip("Interval", review.getIntervalDays() + "d"),
                metaChip("Ease", String.format("%.2f", review.getEaseFactor())),
                metaChip("Reps", String.valueOf(review.getRepetitions())));
        meta2.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(meta2);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(400);
        scroll.setStyle("-fx-background-color: transparent;");

        // wrap in VBox to keep the API clean
        return new VBox(scroll);
    }

    private HBox metaChip(String key, String value) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 12px; -fx-font-weight: bold;");

        VBox chip = new VBox(2, k, v);
        chip.setAlignment(Pos.CENTER_LEFT);
        return new HBox(chip);
    }

    private HBox buildRatingRow() {
        Button again = ratingButton("Again",  "#E74C3C", SM2Scheduler.QUALITY_AGAIN);
        Button hard  = ratingButton("Hard",   "#F39C12", SM2Scheduler.QUALITY_HARD);
        Button good  = ratingButton("Good",   "#27AE60", SM2Scheduler.QUALITY_GOOD);
        Button easy  = ratingButton("Easy",   "#3498DB", SM2Scheduler.QUALITY_EASY);

        HBox row = new HBox(10, again, hard, good, easy);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Button ratingButton(String text, String color, int quality) {
        Button b = new Button(text);
        b.setPrefWidth(110);
        b.setPrefHeight(44);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; "
                + "-fx-font-size: 13px;");
        b.setOnAction(e -> {
            if (onRated != null) onRated.accept(quality);
        });
        return b;
    }

    // ------------------------------------------------------------- reveal

    private void reveal() {
        if (revealed) return;
        revealed = true;

        Object revealRow = answerBox.getProperties().get("revealRow");
        Object ratingRow = answerBox.getProperties().get("ratingRow");
        if (revealRow instanceof Region rr) {
            rr.setVisible(false);
            rr.setManaged(false);
        }
        answerBox.setVisible(true);
        answerBox.setManaged(true);
        if (ratingRow instanceof Region gr) {
            gr.setVisible(true);
            gr.setManaged(true);
        }
    }
}