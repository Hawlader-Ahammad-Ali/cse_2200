package com.mindmap.view.quiz;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.QuizQuestion;
import com.mindmap.model.QuizQuestionType;
import com.mindmap.model.StudySession;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
 * Runs a quiz session. Shows one question at a time, immediate feedback,
 * then advances. Records answers via {@code QuizService}.
 */
public class QuizSessionView extends VBox {

    private static final Logger log = LoggerFactory.getLogger(QuizSessionView.class);

    /** Per-question result snapshot for the results screen. */
    public record Result(QuizQuestion question, String userAnswer, boolean correct) { }

    private final List<QuizQuestion> queue;
    private final List<Result>      results = new ArrayList<>();
    private final Runnable          onExit;
    private final Consumer<List<Result>> onFinished;

    private final Label       progressLabel = new Label();
    private final ProgressBar progressBar   = new ProgressBar(0);
    private final StackPane   cardHolder    = new StackPane();

    private int index = 0;
    private int correctCount = 0;
    private StudySession session;

    // Currently displayed question (for the input state to persist)
    private String pendingAnswer;

    public QuizSessionView(List<QuizQuestion> queue,
                           Runnable onExit,
                           Consumer<List<Result>> onFinished) {
        this.queue = queue;
        this.onExit = onExit;
        this.onFinished = onFinished;

        setSpacing(14);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(20, 40, 20, 40));

        getChildren().addAll(buildTopBar(), buildProgress(), cardHolder);

        try { session = ServiceRegistry.quizService().startSession(); }
        catch (ServiceException e) { log.warn("Could not start session: {}", e.getMessage()); }

        showCurrentQuestion();
    }

    private HBox buildTopBar() {
        Button exitBtn = new Button("✕  End quiz");
        exitBtn.getStyleClass().add("secondary-button");
        exitBtn.setOnAction(e -> {
            if (Dialogs.confirm("End quiz?", "Your answered questions are saved.")) {
                finish();
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

    // ------------------------------------------------------------- question

    private void showCurrentQuestion() {
        if (index >= queue.size()) {
            finish();
            onFinished.accept(results);
            return;
        }
        pendingAnswer = null;
        QuizQuestion q = queue.get(index);
        progressLabel.setText("Question " + (index + 1) + " of " + queue.size());
        progressBar.setProgress((double) index / queue.size());

        cardHolder.getChildren().setAll(buildQuestionCard(q));
    }

    private VBox buildQuestionCard(QuizQuestion q) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setMaxWidth(680);

        Label typeLabel = new Label(q.getQuestionType().getLabel());
        typeLabel.setStyle("-fx-background-color: -fx-accent-soft; -fx-text-fill: -fx-accent-hover; "
                + "-fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");

        Label qText = new Label(q.getQuestionText());
        qText.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");
        qText.setWrapText(true);

        VBox header = new VBox(6, typeLabel, qText);

        // Input area depends on question type
        VBox inputArea = new VBox(8);
        Runnable submit = () -> handleSubmit(q);

        switch (q.getQuestionType()) {
            case MCQ -> {
                ToggleGroup group = new ToggleGroup();
                for (String option : q.getOptions()) {
                    RadioButton rb = new RadioButton(option);
                    rb.setToggleGroup(group);
                    rb.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13.5px;");
                    rb.setOnAction(e -> pendingAnswer = option);
                    inputArea.getChildren().add(rb);
                }
            }
            case TRUE_FALSE -> {
                HBox row = new HBox(12);
                for (String val : List.of("true", "false")) {
                    Button b = new Button(val.substring(0, 1).toUpperCase() + val.substring(1));
                    b.setPrefWidth(140);
                    b.setPrefHeight(42);
                    b.getStyleClass().add("secondary-button");
                    b.setOnAction(e -> { pendingAnswer = val; submit.run(); });
                    row.getChildren().add(b);
                }
                row.setAlignment(Pos.CENTER);
                inputArea.getChildren().add(row);
            }
            case SHORT -> {
                TextField field = new TextField();
                field.setPromptText("Type your answer, then press Enter");
                field.getStyleClass().add("auth-text-field");
                field.setPrefHeight(40);
                field.textProperty().addListener((o, ov, nv) -> pendingAnswer = nv);
                field.setOnAction(e -> submit.run());
                inputArea.getChildren().add(field);
            }
        }

        card.getChildren().addAll(header, new Separator(), inputArea);

        // Submit button for MCQ / SHORT
        if (q.getQuestionType() != QuizQuestionType.TRUE_FALSE) {
            Button submitBtn = new Button("Submit");
            submitBtn.getStyleClass().add("primary-button");
            submitBtn.setPrefWidth(160);
            submitBtn.setPrefHeight(42);
            submitBtn.setOnAction(e -> submit.run());
            HBox row = new HBox(submitBtn);
            row.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(row);
        }

        return card;
    }

    // ------------------------------------------------------------- answer + feedback

    private void handleSubmit(QuizQuestion q) {
        String answer = pendingAnswer;
        if (answer == null || answer.isBlank()) {
            Dialogs.warning("No answer", "Please provide an answer before submitting.");
            return;
        }

        boolean correct;
        try {
            correct = ServiceRegistry.quizService().submitAnswer(q, answer);
        } catch (ServiceException e) {
            Dialogs.error("Could not submit", e.getMessage());
            return;
        }

        if (correct) correctCount++;
        results.add(new Result(q, answer, correct));

        cardHolder.getChildren().setAll(buildFeedbackCard(q, answer, correct));
    }

    private VBox buildFeedbackCard(QuizQuestion q, String userAnswer, boolean correct) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setMaxWidth(680);

        Label icon = new Label(correct ? "✅" : "❌");
        icon.setStyle("-fx-font-size: 40px;");

        Label status = new Label(correct ? "Correct!" : "Not quite");
        status.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; "
                + "-fx-text-fill: " + (correct ? "#27AE60" : "#E74C3C") + ";");

        Label qLabel = new Label(q.getQuestionText());
        qLabel.setWrapText(true);
        qLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text-primary;");

        Label yourAnswer = new Label("Your answer:  " + userAnswer);
        yourAnswer.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-text-secondary;");

        Label correctAnswer = new Label("Correct answer:  " + q.getCorrectAnswer());
        correctAnswer.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        VBox text = new VBox(6, qLabel, yourAnswer, correctAnswer);

        if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
            Label expl = new Label("💡  " + q.getExplanation());
            expl.setWrapText(true);
            expl.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12.5px; "
                    + "-fx-font-style: italic; -fx-padding: 6 0 0 0;");
            text.getChildren().add(expl);
        }

        Button nextBtn = new Button(index < queue.size() - 1 ? "Next Question →" : "See Results");
        nextBtn.getStyleClass().add("primary-button");
        nextBtn.setPrefWidth(220);
        nextBtn.setPrefHeight(42);
        nextBtn.setOnAction(e -> {
            index++;
            showCurrentQuestion();
        });

        HBox nextRow = new HBox(nextBtn);
        nextRow.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(12, icon, status);
        header.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, text, nextRow);
        return card;
    }

    // ------------------------------------------------------------- finish

    private void finish() {
        if (session == null) return;
        try {
            ServiceRegistry.quizService().finishSession(session.getId(), results.size(), correctCount);
        } catch (ServiceException e) {
            log.warn("Could not finish session: {}", e.getMessage());
        }
    }

    public int getCorrectCount() { return correctCount; }
    public int getTotalCount()   { return results.size(); }
}