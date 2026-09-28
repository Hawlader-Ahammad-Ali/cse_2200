package com.mindmap.view.flashcard;

import com.mindmap.ai.dto.SuggestedFlashcard;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Flashcard;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.service.FlashcardService;
import com.mindmap.service.KnowledgeService;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Flashcards landing view: table of cards + actions (new, AI generate, review due).
 */
public class FlashcardsView extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(FlashcardsView.class);

    private final FlashcardService flashcardService = ServiceRegistry.flashcardService();
    private final KnowledgeService knowledgeService = ServiceRegistry.knowledgeService();

    private final ObservableList<Flashcard> data = FXCollections.observableArrayList();
    private final TableView<Flashcard> table = new TableView<>(data);
    private final TextField searchField = new TextField();
    private final CheckBox dueFilter = new CheckBox("Due only");
    private final Label countLabel = new Label("0 cards");
    private final Label dueLabel = new Label();

    private final VBox landing;

    public FlashcardsView() {
        landing = buildLanding();
        getChildren().add(landing);
        refresh();
    }

    // ------------------------------------------------------------- landing

    private VBox buildLanding() {
        Label title = new Label("Flashcards");
        title.getStyleClass().add("view-header");

        countLabel.getStyleClass().add("view-subtitle");
        countLabel.setPadding(new Insets(4, 0, 0, 0));

        VBox titleBox = new VBox(0, title, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button reviewBtn = new Button("🔄  Review Due");
        reviewBtn.getStyleClass().add("primary-button");
        reviewBtn.setOnAction(e -> startReview());

        Button aiBtn = new Button("🤖  AI Generate");
        aiBtn.getStyleClass().add("secondary-button");
        aiBtn.setOnAction(e -> openAIGenerateDialog());

        Button addBtn = new Button("+  New Card");
        addBtn.getStyleClass().add("secondary-button");
        addBtn.setOnAction(e -> openCreateDialog());

        HBox header = new HBox(8, titleBox, spacer, reviewBtn, aiBtn, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));

        searchField.getStyleClass().add("source-search");
        searchField.setPromptText("🔍  Search question or answer…");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((o, ov, nv) -> refresh());

        dueFilter.getStyleClass().add("takeaway-filter-checkbox");
        dueFilter.selectedProperty().addListener((o, ov, nv) -> refresh());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            dueFilter.setSelected(false);
        });

        dueLabel.getStyleClass().add("view-subtitle");
        dueLabel.setStyle("-fx-font-size: 11.5px;");

        HBox filters = new HBox(10, searchField, dueFilter, clearBtn, dueLabel);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(4, 0, 8, 0));

        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox root = new VBox(6, header, filters, table);
        root.setPadding(new Insets(4, 0, 0, 0));
        return root;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No flashcards yet — click \"New Card\" or \"AI Generate\"."));

        TableColumn<Flashcard, String> qCol = new TableColumn<>("Question");
        qCol.setCellValueFactory(cd -> {
            String q = cd.getValue().getQuestion();
            if (q == null) q = "";
            String snippet = q.length() > 70 ? q.substring(0, 67) + "…" : q;
            return new SimpleStringProperty(snippet);
        });
        qCol.setPrefWidth(340);

        TableColumn<Flashcard, String> aCol = new TableColumn<>("Answer");
        aCol.setCellValueFactory(cd -> {
            String a = cd.getValue().getAnswer();
            if (a == null) a = "";
            String snippet = a.length() > 50 ? a.substring(0, 47) + "…" : a;
            return new SimpleStringProperty(snippet);
        });
        aCol.setPrefWidth(280);

        TableColumn<Flashcard, String> linkCol = new TableColumn<>("Linked");
        linkCol.setCellValueFactory(cd -> {
            Integer kid = cd.getValue().getKnowledgeItemId();
            if (kid == null) return new SimpleStringProperty("");
            try {
                Optional<KnowledgeItem> k = knowledgeService.getById(kid);
                return new SimpleStringProperty(k.map(KnowledgeItem::getTitle).orElse("(missing)"));
            } catch (ServiceException e) {
                return new SimpleStringProperty("(error)");
            }
        });
        linkCol.setPrefWidth(180);

        TableColumn<Flashcard, String> originCol = new TableColumn<>("Origin");
        originCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrigin().getIcon() + " " + cd.getValue().getOrigin().getLabel()));
        originCol.setPrefWidth(140);

        table.getColumns().addAll(qCol, aCol, linkCol, originCol);

        table.setRowFactory(tv -> {
            TableRow<Flashcard> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog(row.getItem());
                }
            });
            return row;
        });

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            Flashcard sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openEditDialog(sel);
        });
        MenuItem delItem = new MenuItem("Delete");
        delItem.setOnAction(e -> {
            Flashcard sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) confirmDelete(sel);
        });
        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        table.setContextMenu(menu);
    }

    // ------------------------------------------------------------- refresh

    private void refresh() {
        try {
            List<Flashcard> rows;
            String q = searchField.getText();

            if (dueFilter.isSelected()) {
                rows = flashcardService.getDue();
                if (q != null && !q.isBlank()) {
                    String lower = q.toLowerCase();
                    rows = rows.stream().filter(f ->
                            contains(f.getQuestion(), lower) || contains(f.getAnswer(), lower)
                    ).toList();
                }
            } else if (q != null && !q.isBlank()) {
                rows = flashcardService.search(q);
            } else {
                rows = flashcardService.getAll();
            }

            data.setAll(rows);
            countLabel.setText(data.size() + (data.size() == 1 ? " card" : " cards"));

            int due = flashcardService.countDue();
            dueLabel.setText(due > 0 ? "(" + due + " due)" : "");

        } catch (ServiceException e) {
            Dialogs.error("Could not load flashcards", e.getMessage());
        }
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    // ------------------------------------------------------------- dialogs

    private void openCreateDialog() {
        List<KnowledgeItem> items;
        try { items = knowledgeService.getAll(); }
        catch (ServiceException e) { items = List.of(); }

        FlashcardFormDialog dlg = new FlashcardFormDialog(null, items);
        dlg.showAndWait().ifPresent(f -> {
            try {
                flashcardService.create(f);
                AppContext.getInstance().setStatusMessage("Flashcard created.");
                refresh();
            } catch (ServiceException e) {
                Dialogs.error("Could not save card", e.getMessage());
            }
        });
    }

    private void openEditDialog(Flashcard card) {
        List<KnowledgeItem> items;
        try { items = knowledgeService.getAll(); }
        catch (ServiceException e) { items = List.of(); }

        FlashcardFormDialog dlg = new FlashcardFormDialog(card, items);
        dlg.showAndWait().ifPresent(f -> {
            try {
                flashcardService.update(f);
                AppContext.getInstance().setStatusMessage("Flashcard updated.");
                refresh();
            } catch (ServiceException e) {
                Dialogs.error("Could not update card", e.getMessage());
            }
        });
    }

    private void confirmDelete(Flashcard card) {
        boolean ok = Dialogs.confirm("Delete this flashcard?",
                "\"" + snippet(card.getQuestion()) + "\" will be permanently removed.");
        if (!ok) return;

        try {
            flashcardService.delete(card.getId());
            AppContext.getInstance().setStatusMessage("Flashcard deleted.");
            refresh();
        } catch (ServiceException e) {
            Dialogs.error("Could not delete card", e.getMessage());
        }
    }

    private String snippet(String s) {
        if (s == null) return "";
        return s.length() > 40 ? s.substring(0, 37) + "…" : s;
    }

    // ------------------------------------------------------------- AI

    private void openAIGenerateDialog() {
        List<KnowledgeItem> items;
        try { items = knowledgeService.getAll(); }
        catch (ServiceException e) { items = List.of(); }

        if (items.isEmpty()) {
            Dialogs.info("No knowledge items",
                    "Add at least one knowledge item before generating flashcards.");
            return;
        }

        ChoiceDialog<KnowledgeItem> pick = new ChoiceDialog<>(items.get(0), items);
        pick.setTitle("Generate Flashcards");
        pick.setHeaderText("Which concept should the AI create cards for?");
        pick.setContentText("Knowledge item:");
        applyStylesheet(pick.getDialogPane());
        pick.showAndWait().ifPresent(this::runAIGeneration);
    }

    /**
     * Bulletproof AI generation:
     *  - catches Throwable (not just ServiceException)
     *  - has a 60-second timeout so the dialog always closes
     *  - logs every step for diagnostics
     */
    private void runAIGeneration(KnowledgeItem item) {
        log.info("AI flashcard generation requested for item id={} title='{}'",
                item.getId(), item.getTitle());

        final Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("AI is thinking…");
        progress.setHeaderText("Generating flashcards for: " + item.getTitle());
        progress.setContentText("This may take a few seconds.");
        progress.getDialogPane().getButtonTypes().clear();
        progress.setResizable(false);
        applyStylesheet(progress.getDialogPane());

        // Atomic done flag so timeout + worker don't double-fire
        final java.util.concurrent.atomic.AtomicBoolean done =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        // --- 60s timeout guard ---
        Thread timeout = new Thread(() -> {
            try { Thread.sleep(60_000); } catch (InterruptedException e) { return; }
            if (done.compareAndSet(false, true)) {
                log.warn("AI flashcard generation timed out after 60s");
                Platform.runLater(() -> {
                    safeClose(progress);
                    Dialogs.error("AI generation timed out",
                            "The AI did not respond within 60 seconds. "
                                    + "Check your internet connection, or run in offline mode "
                                    + "(unset MINDMAP_AI_KEY).");
                });
            }
        }, "ai-flashcard-timeout");
        timeout.setDaemon(true);
        timeout.start();

        // --- worker ---
        Thread worker = new Thread(() -> {
            try {
                log.info("Worker: calling AIService.generateFlashcards…");
                List<SuggestedFlashcard> suggestions =
                        ServiceRegistry.aiService().generateFlashcards(item);
                log.info("Worker: AI returned {} suggestions", suggestions.size());

                if (done.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        safeClose(progress);

                        if (suggestions.isEmpty()) {
                            Dialogs.warning("No suggestions",
                                    "The AI returned no flashcards. Try a different concept.");
                            return;
                        }

                        AIFlashcardDialog dlg = new AIFlashcardDialog(item, suggestions);
                        dlg.showAndWait().ifPresent(created -> {
                            AppContext.getInstance().setStatusMessage(
                                    created.size() + " flashcard(s) created.");
                            refresh();
                        });
                    });
                }
            } catch (Throwable t) {
                log.error("AI flashcard generation failed", t);
                if (done.compareAndSet(false, true)) {
                    String msg = t.getMessage();
                    if (msg == null || msg.isBlank()) msg = t.getClass().getSimpleName();
                    final String finalMsg = msg;
                    Platform.runLater(() -> {
                        safeClose(progress);
                        Dialogs.error("AI generation failed", finalMsg);
                    });
                }
            }
        }, "ai-flashcard-worker");
        worker.setDaemon(true);
        worker.start();

        log.info("Worker started; showing progress dialog");
        progress.show();
    }

    private void safeClose(Alert alert) {
        try { alert.close(); } catch (Exception ignore) { }
    }

    private void applyStylesheet(DialogPane pane) {
        try {
            var css = getClass().getResource("/css/app.css");
            if (css != null) pane.getStylesheets().add(css.toExternalForm());
            String theme = AppContext.getInstance().isDarkTheme()
                    ? "/css/theme-dark.css" : "/css/theme-light.css";
            var tcss = getClass().getResource(theme);
            if (tcss != null) pane.getStylesheets().add(tcss.toExternalForm());
        } catch (Exception ignore) { }
    }

    // ------------------------------------------------------------- review

    private void startReview() {
        List<Flashcard> due;
        try { due = flashcardService.getDue(); }
        catch (ServiceException e) {
            Dialogs.error("Could not load queue", e.getMessage());
            return;
        }
        if (due.isEmpty()) {
            Dialogs.info("Nothing to review", "No flashcards are due right now.");
            return;
        }

        FlashcardReviewSession session = new FlashcardReviewSession(
                due,
                this::returnToLanding,
                n -> AppContext.getInstance().setStatusMessage("Reviewed " + n + " flashcard(s).")
        );
        getChildren().setAll(session);
    }

    private void returnToLanding() {
        getChildren().setAll(landing);
        refresh();
    }
}