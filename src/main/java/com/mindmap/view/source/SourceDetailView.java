package com.mindmap.view.source;

import com.mindmap.ai.dto.SuggestedTopic;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import com.mindmap.view.knowledge.TakeawayEditorDialog;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import com.mindmap.export.ExportButtonHelper;

/**
 * Read-only detail view for a single source.
 *
 * <p>Phase 10: the "My Takeaways" section is now live. It aggregates all
 * personal notes from knowledge items linked to this source, and lets the
 * user edit any takeaway via {@link TakeawayEditorDialog}.</p>
 */
public class SourceDetailView extends VBox {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final Source source;
    private final Consumer<Source> onEdit;
    private final Runnable onDelete;
    private final Runnable onBack;
    private final Consumer<KnowledgeItem> onOpenKnowledge;

    private final Button analyzeBtn    = new Button("🤖  Analyze with AI");
    private final Label  analyzeStatus = new Label();

    // Cache so we can re-render the takeaway card after edits without a full rebuild
    private VBox takeawayCard;

    public SourceDetailView(Source source,
                            Consumer<Source> onEdit,
                            Runnable onDelete,
                            Runnable onBack,
                            Consumer<KnowledgeItem> onOpenKnowledge) {
        this.source = source;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onBack = onBack;
        this.onOpenKnowledge = onOpenKnowledge;

        setSpacing(16);
        setPadding(new Insets(0));

        getChildren().addAll(
                buildHeader(),
                buildScrollableBody()
        );
    }

    // ------------------------------------------------------------- header

    private HBox buildHeader() {
        Button backBtn = new Button("←  Back to Sources");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = ExportButtonHelper.sourceExportButton(source);

        Button editBtn = new Button("✎  Edit");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> onEdit.accept(source));

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> onDelete.run());

        HBox header = new HBox(10, backBtn, spacer, exportBtn, editBtn, deleteBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ------------------------------------------------------------- body

    private ScrollPane buildScrollableBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(4, 2, 4, 2));

        takeawayCard = buildTakeawayCard();   // built once, refreshed on edit

        body.getChildren().addAll(
                buildTitleCard(),
                buildInfoCard(),
                buildAICard(),
                buildRelatedKnowledgeCard(),
                takeawayCard
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private VBox buildTitleCard() {
        Label icon = new Label(source.getSourceType().getIcon());
        icon.setStyle("-fx-font-size: 34px;");

        Label title = new Label(source.getTitle());
        title.getStyleClass().add("view-header");
        title.setWrapText(true);

        Label sub = new Label(source.getSourceType().getLabel()
                + (source.getAuthorCreator() != null
                ? "  ·  " + source.getAuthorCreator() : ""));
        sub.getStyleClass().add("view-subtitle");

        VBox text = new VBox(2, title, sub);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(14, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildInfoCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label header = new Label("Source information");
        header.getStyleClass().add("section-header");
        card.getChildren().add(header);

        if (source.getDescription() != null && !source.getDescription().isBlank()) {
            Label desc = new Label(source.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: -fx-text-secondary;");
            card.getChildren().add(desc);
        }

        card.getChildren().add(new Separator());
        card.getChildren().add(infoRow("Status", source.getStatus().getLabel()));
        if (source.getRating() != null) {
            card.getChildren().add(infoRow("Rating", "★".repeat(source.getRating())));
        }
        if (source.getDateAdded() != null) {
            card.getChildren().add(infoRow("Added",
                    source.getDateAdded().toLocalDate().format(DATE_FMT)));
        }
        if (source.getDateConsumed() != null) {
            card.getChildren().add(infoRow("Consumed", source.getDateConsumed().format(DATE_FMT)));
        }
        if (source.getUrl() != null && !source.getUrl().isBlank()) {
            card.getChildren().add(infoRow("URL", source.getUrl()));
        }
        if (!source.getTags().isEmpty()) {
            FlowPane tagFlow = new FlowPane(6, 6);
            for (Tag t : source.getTags()) {
                Label chip = new Label(t.getName());
                chip.getStyleClass().add("tag-chip");
                tagFlow.getChildren().add(chip);
            }
            card.getChildren().add(infoRow("Tags", tagFlow));
        }
        if (source.getNotes() != null && !source.getNotes().isBlank()) {
            Label notes = new Label(source.getNotes());
            notes.setWrapText(true);
            notes.setStyle("-fx-text-fill: -fx-text-secondary;");
            card.getChildren().add(infoRow("Notes", notes));
        }
        return card;
    }

    private VBox buildAICard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label header = new Label("🤖  AI Suggested Learning");
        header.getStyleClass().add("section-header");

        Label desc = new Label("Ask the AI to suggest POSSIBLE learning themes from this source. "
                + "You review each suggestion and decide what becomes part of your knowledge base.");
        desc.setWrapText(true);
        desc.getStyleClass().add("placeholder-desc");

        analyzeBtn.getStyleClass().add("primary-button");
        analyzeBtn.setOnAction(e -> runAnalysis());

        analyzeStatus.getStyleClass().add("view-subtitle");
        analyzeStatus.setPadding(new Insets(2, 0, 0, 4));

        HBox actions = new HBox(10, analyzeBtn, analyzeStatus);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, desc, new Separator(), actions);
        return card;
    }

    private void runAnalysis() {
        analyzeBtn.setDisable(true);
        analyzeStatus.setText("Analyzing… this may take a few seconds.");

        new Thread(() -> {
            try {
                List<SuggestedTopic> topics =
                        ServiceRegistry.aiService().analyzeSource(source);

                Platform.runLater(() -> {
                    analyzeStatus.setText(topics.size() + " suggestions received.");
                    analyzeBtn.setDisable(false);

                    AIAnalysisDialog dlg = new AIAnalysisDialog(source, topics);
                    dlg.showAndWait().ifPresent(created -> {
                        analyzeStatus.setText(created.size() + " items added to your knowledge.");
                        refreshTakeawayCard();
                    });
                });
            } catch (ServiceException ex) {
                Platform.runLater(() -> {
                    analyzeStatus.setText("Failed.");
                    analyzeBtn.setDisable(false);
                    Dialogs.error("AI analysis failed", ex.getMessage());
                });
            }
        }, "ai-analysis-worker").start();
    }

    private VBox buildRelatedKnowledgeCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label header = new Label("🔗  Related Knowledge");
        header.getStyleClass().add("section-header");
        card.getChildren().add(header);

        List<KnowledgeItem> items;
        try {
            items = ServiceRegistry.knowledgeService().getKnowledgeForSource(source.getId());
        } catch (ServiceException e) {
            Label err = new Label("Could not load related knowledge.");
            err.getStyleClass().add("placeholder-desc");
            card.getChildren().add(err);
            return card;
        }

        if (items.isEmpty()) {
            Label empty = new Label("No knowledge items linked to this source yet. "
                    + "Use the AI button above to generate suggestions, or add items manually.");
            empty.getStyleClass().add("placeholder-desc");
            empty.setWrapText(true);
            card.getChildren().add(empty);
            return card;
        }

        for (KnowledgeItem k : items) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));

            Label icon = new Label(k.getItemType().getIcon());
            icon.setStyle("-fx-font-size: 18px;");

            Label title = new Label(k.getTitle());
            title.getStyleClass().add("source-info-value");
            HBox.setHgrow(title, Priority.ALWAYS);

            Label origin = new Label(k.getOrigin().getDisplay());
            origin.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px;");

            row.getChildren().addAll(icon, title, origin);
            row.setOnMouseClicked(e -> {
                if (onOpenKnowledge != null) onOpenKnowledge.accept(k);
            });
            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-cursor: hand; -fx-background-color: -fx-bg-tertiary; -fx-background-radius: 6;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand;"));

            card.getChildren().add(row);
        }
        return card;
    }

    // ------------------------------------------------------------- takeaways (live)

    private VBox buildTakeawayCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label header = new Label("📝  My Takeaways");
        header.getStyleClass().add("section-header");
        card.getChildren().add(header);

        List<KnowledgeItem> withTakeaways;
        try {
            List<KnowledgeItem> allItems =
                    ServiceRegistry.knowledgeService().getKnowledgeForSource(source.getId());
            withTakeaways = allItems.stream()
                    .filter(k -> k.getPersonalNote() != null && !k.getPersonalNote().isBlank())
                    .toList();
        } catch (ServiceException e) {
            Label err = new Label("Could not load takeaways.");
            err.getStyleClass().add("placeholder-desc");
            card.getChildren().add(err);
            return card;
        }

        int totalItems;
        try {
            totalItems = ServiceRegistry.knowledgeService()
                    .getKnowledgeForSource(source.getId()).size();
        } catch (ServiceException e) {
            totalItems = 0;
        }

        if (withTakeaways.isEmpty()) {
            Label empty = new Label("You haven't written any takeaways yet for the concepts "
                    + "linked to this source. Open a knowledge item and use the 📝 Edit Takeaway "
                    + "button, or click a related item above to add yours.");
            empty.getStyleClass().add("placeholder-desc");
            empty.setWrapText(true);
            card.getChildren().add(empty);
            return card;
        }

        // Summary line
        Label summary = new Label(withTakeaways.size() + " of " + totalItems
                + " linked item" + (totalItems == 1 ? "" : "s")
                + " ha" + (totalItems == 1 ? "s" : "ve") + " a personal takeaway.");
        summary.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");
        card.getChildren().add(summary);

        card.getChildren().add(new Separator());

        // Each takeaway as an editable card
        for (KnowledgeItem k : withTakeaways) {
            VBox row = new VBox(4);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle("-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8;");

            // Header row: icon + title + edit button
            Label icon = new Label(k.getItemType().getIcon());
            icon.setStyle("-fx-font-size: 15px;");

            Label title = new Label(k.getTitle());
            title.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 12.5px; -fx-font-weight: bold;");
            title.setWrapText(true);
            HBox.setHgrow(title, Priority.ALWAYS);

            Button editBtn = new Button("✎");
            editBtn.getStyleClass().add("takeaway-edit-button");
            editBtn.setTooltip(new javafx.scene.control.Tooltip("Edit this takeaway"));
            editBtn.setOnAction(e -> openTakeawayEditor(k));

            HBox titleRow = new HBox(6, icon, title, editBtn);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            // The takeaway body
            Label body = new Label(k.getPersonalNote());
            body.setWrapText(true);
            body.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12.5px; -fx-padding: 2 0 0 22;");

            row.getChildren().addAll(titleRow, body);

            // Hover effect
            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-background-color: -fx-accent-soft; -fx-background-radius: 8;"));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8;"));

            card.getChildren().add(row);
        }

        return card;
    }

    /** Replaces the takeaway card in the body with a freshly built one. */
    private void refreshTakeawayCard() {
        if (takeawayCard == null) return;
        VBox parent = (VBox) takeawayCard.getParent();
        if (parent == null) return;
        int idx = parent.getChildren().indexOf(takeawayCard);
        if (idx < 0) return;

        takeawayCard = buildTakeawayCard();
        parent.getChildren().set(idx, takeawayCard);
    }

    private void openTakeawayEditor(KnowledgeItem item) {
        TakeawayEditorDialog dlg = new TakeawayEditorDialog(item);
        dlg.showAndWait().ifPresent(saved -> {
            refreshTakeawayCard();
            // Also notify the caller so it can refresh if needed
            if (onOpenKnowledge != null) {
                // No-op — just giving the caller a hook if they want to do something
            }
        });
    }

    // ------------------------------------------------------------- helpers

    private HBox infoRow(String key, String value) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("source-info-key");
        k.setMinWidth(90);
        Label v = new Label(value);
        v.getStyleClass().add("source-info-value");
        v.setWrapText(true);
        HBox row = new HBox(8, k, v);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private HBox infoRow(String key, javafx.scene.Node node) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("source-info-key");
        k.setMinWidth(90);
        HBox row = new HBox(8, k, node);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }
}