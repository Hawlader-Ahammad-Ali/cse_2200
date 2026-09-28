package com.mindmap.view.quiz;

import com.mindmap.ai.dto.SuggestedQuizQuestion;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.QuizQuestion;
import com.mindmap.service.KnowledgeService;
import com.mindmap.service.QuizService;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.BusyIndicator;
import com.mindmap.util.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quiz landing page.
 * AI generation uses a disabled-button progress indicator — no Alert dialogs,
 * no possibility of the dialog hanging the FX thread.
 */
public class QuizView extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(QuizView.class);
    private static final String AI_BUTTON_IDLE = "🤖  AI Generate";
    private static final String AI_BUTTON_BUSY = "⏳  Generating…";

    private final QuizService      quizService      = ServiceRegistry.quizService();
    private final KnowledgeService knowledgeService = ServiceRegistry.knowledgeService();

    private final ObservableList<QuizQuestion> data = FXCollections.observableArrayList();
    private final TableView<QuizQuestion> table = new TableView<>(data);
    private final Label statsLabel      = new Label();
    private final Label questionsCount  = new Label("0 questions");
    private final BusyIndicator busyIndicator = new BusyIndicator(null);

    /** Held as a field so runAIGeneration can toggle its state. */
    private Button aiGenerateBtn;

    private final VBox landing;

    public QuizView() {
        landing = buildLanding();
        getChildren().add(landing);
        refresh();
    }

    // ------------------------------------------------------------- landing

    private VBox buildLanding() {
        Label title = new Label("Quiz");
        title.getStyleClass().add("view-header");
        questionsCount.getStyleClass().add("view-subtitle");
        questionsCount.setPadding(new Insets(4, 0, 0, 0));
        VBox titleBox = new VBox(0, title, questionsCount);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button startBtn = new Button("▶  Start Quiz");
        startBtn.getStyleClass().add("primary-button");
        startBtn.setOnAction(e -> openSetupDialog());

        aiGenerateBtn = new Button(AI_BUTTON_IDLE);
        aiGenerateBtn.getStyleClass().add("secondary-button");
        aiGenerateBtn.setOnAction(e -> openAIGenerateDialog());

        Button newBtn = new Button("+  New Question");
        newBtn.getStyleClass().add("secondary-button");
        newBtn.setOnAction(e -> openNewQuestionDialog());

        HBox header = new HBox(8, titleBox, spacer, busyIndicator, startBtn, aiGenerateBtn, newBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));

        statsLabel.getStyleClass().add("view-subtitle");
        statsLabel.setStyle("-fx-font-size: 12.5px;");
        updateStats();
        HBox statsRow = new HBox(statsLabel);
        statsRow.setPadding(new Insets(0, 0, 8, 0));

        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox root = new VBox(6, header, statsRow, table);
        root.setPadding(new Insets(4, 0, 0, 0));
        return root;
    }

    private void updateStats() {
        try {
            int total = quizService.countQuestions();
            List<QuizQuestion> weak = quizService.getWeakQuestions();
            statsLabel.setText(total + " question" + (total == 1 ? "" : "s")
                    + " in your pool   ·   " + weak.size() + " weak");
        } catch (ServiceException e) {
            statsLabel.setText("");
        }
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No questions yet — click \"New Question\" or \"AI Generate\"."));

        TableColumn<QuizQuestion, String> qCol = new TableColumn<>("Question");
        qCol.setCellValueFactory(cd -> {
            String q = cd.getValue().getQuestionText();
            if (q == null) q = "";
            String s = q.length() > 80 ? q.substring(0, 77) + "…" : q;
            return new SimpleStringProperty(s);
        });
        qCol.setPrefWidth(420);

        TableColumn<QuizQuestion, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getQuestionType().getLabel()));
        typeCol.setPrefWidth(130);

        TableColumn<QuizQuestion, String> answerCol = new TableColumn<>("Correct");
        answerCol.setCellValueFactory(cd -> {
            String a = cd.getValue().getCorrectAnswer();
            if (a == null) a = "";
            String s = a.length() > 40 ? a.substring(0, 37) + "…" : a;
            return new SimpleStringProperty(s);
        });
        answerCol.setPrefWidth(220);

        TableColumn<QuizQuestion, String> originCol = new TableColumn<>("Origin");
        originCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrigin().getIcon() + " " + cd.getValue().getOrigin().getLabel()));
        originCol.setPrefWidth(130);

        table.getColumns().addAll(qCol, typeCol, answerCol, originCol);

        table.setRowFactory(tv -> {
            TableRow<QuizQuestion> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditQuestionDialog(row.getItem());
                }
            });
            return row;
        });

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            QuizQuestion sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openEditQuestionDialog(sel);
        });
        MenuItem delItem = new MenuItem("Delete");
        delItem.setOnAction(e -> {
            QuizQuestion sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) confirmDelete(sel);
        });
        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        table.setContextMenu(menu);
    }

    private void refresh() {
        try {
            data.setAll(quizService.getAllQuestions());
            questionsCount.setText(data.size() + (data.size() == 1 ? " question" : " questions"));
            updateStats();
        } catch (ServiceException e) {
            Dialogs.error("Could not load questions", e.getMessage());
        }
    }

    // ------------------------------------------------------------- dialogs

    private void openSetupDialog() {
        int available;
        try { available = quizService.countQuestions(); }
        catch (ServiceException e) { available = 0; }

        if (available == 0) {
            Dialogs.info("No questions yet",
                    "Add questions manually, or use AI Generate to create some.");
            return;
        }

        QuizSetupDialog dlg = new QuizSetupDialog(available);
        dlg.showAndWait().ifPresent(setup -> {
            try {
                List<QuizQuestion> queue = quizService.buildQuiz(setup.count(), setup.weakFirst());
                if (queue.isEmpty()) {
                    Dialogs.info("Empty queue", "No questions matched your filter.");
                    return;
                }
                QuizSessionView session = new QuizSessionView(
                        queue, this::returnToLanding, this::onSessionFinished);
                getChildren().setAll(session);
            } catch (ServiceException e) {
                Dialogs.error("Could not start quiz", e.getMessage());
            }
        });
    }

    private void onSessionFinished(List<QuizSessionView.Result> results) {
        QuizResultsView resultsView = new QuizResultsView(results, this::returnToLanding);
        getChildren().setAll(resultsView);
    }

    private void returnToLanding() {
        getChildren().setAll(landing);
        refresh();
    }

    private void openNewQuestionDialog() {
        List<KnowledgeItem> items;
        try { items = knowledgeService.getAll(); }
        catch (ServiceException e) { items = List.of(); }

        QuizQuestionFormDialog dlg = new QuizQuestionFormDialog(null, items);
        dlg.showAndWait().ifPresent(q -> {
            try {
                quizService.createQuestion(q);
                AppContext.getInstance().setStatusMessage("Question created.");
                refresh();
            } catch (ServiceException e) {
                Dialogs.error("Could not save question", e.getMessage());
            }
        });
    }

    private void openEditQuestionDialog(QuizQuestion q) {
        List<KnowledgeItem> items;
        try { items = knowledgeService.getAll(); }
        catch (ServiceException e) { items = List.of(); }

        QuizQuestionFormDialog dlg = new QuizQuestionFormDialog(q, items);
        dlg.showAndWait().ifPresent(updated -> {
            try {
                quizService.updateQuestion(updated);
                AppContext.getInstance().setStatusMessage("Question updated.");
                refresh();
            } catch (ServiceException e) {
                Dialogs.error("Could not update question", e.getMessage());
            }
        });
    }

    private void confirmDelete(QuizQuestion q) {
        boolean ok = Dialogs.confirm("Delete this question?",
                "This question will be permanently removed.");
        if (!ok) return;
        try {
            quizService.deleteQuestion(q.getId());
            AppContext.getInstance().setStatusMessage("Question deleted.");
            refresh();
        } catch (ServiceException e) {
            Dialogs.error("Could not delete", e.getMessage());
        }
    }

    // ------------------------------------------------------------- AI

    private void openAIGenerateDialog() {
        List<KnowledgeItem> items;
        try { items = knowledgeService.getAll(); }
        catch (ServiceException e) { items = List.of(); }

        if (items.isEmpty()) {
            Dialogs.info("No knowledge items",
                    "Add at least one knowledge item before generating questions.");
            return;
        }

        ChoiceDialog<KnowledgeItem> pick = new ChoiceDialog<>(items.get(0), items);
        pick.setTitle("Generate Quiz Questions");
        pick.setHeaderText("Which concept should the AI create questions for?");
        pick.setContentText("Knowledge item:");
        applyStylesheet(pick.getDialogPane());
        pick.showAndWait().ifPresent(this::runAIGeneration);
    }

    /**
     * Bulletproof AI generation:
     *  - No progress Alert dialog (that's where the hang was).
     *  - The AI Generate button itself shows the progress state.
     *  - Background worker catches Throwable, always restores button state.
     *  - 45-second timeout guard so the button never stays stuck.
     */
    
    private void runAIGeneration(KnowledgeItem item) {
        log.info("AI quiz generation requested for item id={} title='{}'",
                item.getId(), item.getTitle());

        if (aiGenerateBtn != null) {
            aiGenerateBtn.setDisable(true);
        }
        busyIndicator.start("Generating...");

        Thread worker = new Thread(() -> {
            try {
                List<SuggestedQuizQuestion> suggestions =
                        ServiceRegistry.aiService().generateQuizQuestions(item);

                Platform.runLater(() -> {
                    busyIndicator.stop();
                    restoreAIButton();

                    if (suggestions.isEmpty()) {
                        Dialogs.warning("No suggestions", "The AI returned no questions.");
                        return;
                    }

                    try {
                        AIQuizDialog dlg = new AIQuizDialog(item, suggestions);
                        dlg.showAndWait().ifPresent(created -> {
                            AppContext.getInstance().setStatusMessage(created.size() + " question(s) created.");
                            refresh();
                        });
                    } catch (Throwable t) {
                        Dialogs.error("Could not open dialog", t.getMessage());
                    }
                });
            } catch (Throwable t) {
                Platform.runLater(() -> {
                    busyIndicator.stop();
                    restoreAIButton();
                    String msg = t.getMessage();
                    Dialogs.error("AI generation failed", msg != null ? msg : "Unknown error");
                });
            }
        }, "ai-quiz-worker");
        worker.setDaemon(true);
        worker.start();
    }
private void restoreAIButton() {
        if (aiGenerateBtn != null) {
            aiGenerateBtn.setDisable(false);
            aiGenerateBtn.setText(AI_BUTTON_IDLE);
        }
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