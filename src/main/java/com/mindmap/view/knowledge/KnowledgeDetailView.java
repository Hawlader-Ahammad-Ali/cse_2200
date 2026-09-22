package com.mindmap.view.knowledge;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

/**
 * Read-only detail view for a knowledge item, including the
 * "Where Did I Learn This?" section (linked sources).
 */
public class KnowledgeDetailView extends VBox {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final KnowledgeItem item;
    private final Consumer<KnowledgeItem> onEdit;
    private final Runnable onDelete;
    private final Runnable onBack;

    public KnowledgeDetailView(KnowledgeItem item,
                               Consumer<KnowledgeItem> onEdit,
                               Runnable onDelete,
                               Runnable onBack) {
        this.item = item;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onBack = onBack;

        setSpacing(16);

        getChildren().addAll(
                buildHeader(),
                buildScrollableBody()
        );
    }

    // ------------------------------------------------------------- header

    private HBox buildHeader() {
        Button backBtn = new Button("←  Back to Knowledge");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✎  Edit");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> onEdit.accept(item));

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> onDelete.run());

        HBox header = new HBox(10, backBtn, spacer, editBtn, deleteBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ------------------------------------------------------------- body

    private ScrollPane buildScrollableBody() {
        VBox body = new VBox(14);
        body.setPadding(new Insets(4, 2, 4, 2));

        body.getChildren().addAll(
                buildTitleCard(),
                buildInfoCard(),
                buildTakeawayCard(),
                buildWhereLearnedCard(),
                buildPlaceholderCard("🕸  Graph Connections",
                        "Connections to other knowledge items will appear here. "
                                + "You will be able to draw, edit, and remove edges on the graph page.",
                        "Phase 8 — Knowledge Graph"),
                buildPlaceholderCard("🔄  Review History",
                        "Spaced repetition schedule and past reviews will appear here.",
                        "Phase 11 — Spaced Repetition")
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private VBox buildTitleCard() {
        Label icon = new Label(item.getItemType().getIcon());
        icon.setStyle("-fx-font-size: 34px;");

        Label title = new Label(item.getTitle());
        title.getStyleClass().add("view-header");
        title.setWrapText(true);

        Label sub = new Label(item.getItemType().getLabel()
                + (item.getCategory() != null ? "  ·  " + item.getCategory() : "")
                + "  ·  " + item.getOrigin().getDisplay());
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

        Label header = new Label("Details");
        header.getStyleClass().add("section-header");
        card.getChildren().add(header);

        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            Label desc = new Label(item.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: -fx-text-secondary;");
            card.getChildren().add(desc);
        }

        card.getChildren().add(new Separator());
        card.getChildren().add(infoRow("Difficulty", item.getDifficulty()));
        card.getChildren().add(infoRow("Importance", item.getImportance()));
        card.getChildren().add(infoRow("Confidence", "★".repeat(item.getConfidence())
                + "☆".repeat(5 - item.getConfidence())));

        if (item.getDateLearned() != null) {
            card.getChildren().add(infoRow("Date learned", item.getDateLearned().format(DATE_FMT)));
        }
        if (item.getCreatedAt() != null) {
            card.getChildren().add(infoRow("Added", item.getCreatedAt().toLocalDate().format(DATE_FMT)));
        }
        if (!item.getTags().isEmpty()) {
            FlowPane tags = new FlowPane(6, 6);
            for (Tag t : item.getTags()) {
                Label chip = new Label(t.getName());
                chip.getStyleClass().add("tag-chip");
                tags.getChildren().add(chip);
            }
            card.getChildren().add(infoRow("Tags", tags));
        }
        return card;
    }

    private VBox buildTakeawayCard() {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");

        Label header = new Label("📝  My Takeaway");
        header.getStyleClass().add("section-header");

        Label body;
        if (item.getPersonalNote() == null || item.getPersonalNote().isBlank()) {
            body = new Label("No takeaway recorded yet. Click Edit to add your personal lesson.");
            body.getStyleClass().add("placeholder-desc");
        } else {
            body = new Label(item.getPersonalNote());
            body.setWrapText(true);
            body.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13px;");
        }

        card.getChildren().addAll(header, body);
        return card;
    }

    private VBox buildWhereLearnedCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label header = new Label("📍  Where Did I Learn This?");
        header.getStyleClass().add("section-header");
        card.getChildren().add(header);

        List<Source> sources;
        try {
            sources = ServiceRegistry.knowledgeService().getSourcesFor(item.getId());
        } catch (ServiceException e) {
            Dialogs.error("Could not load sources", e.getMessage());
            card.getChildren().add(new Label("Could not load source links."));
            return card;
        }

        if (sources.isEmpty()) {
            Label empty = new Label("Not linked to any source yet. "
                    + "Edit this item to link it to the sources you learned it from.");
            empty.getStyleClass().add("placeholder-desc");
            empty.setWrapText(true);
            card.getChildren().add(empty);
            return card;
        }

        Label summary = new Label(
                "You have explored this concept through " + sources.size()
                        + (sources.size() == 1 ? " source." : " different sources."));
        summary.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12.5px;");
        card.getChildren().add(summary);

        for (Source s : sources) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));

            Label icon = new Label(s.getSourceType().getIcon());
            icon.setStyle("-fx-font-size: 20px;");

            Label title = new Label(s.getTitle());
            title.getStyleClass().add("source-info-value");
            HBox.setHgrow(title, Priority.ALWAYS);

            Label type = new Label(s.getSourceType().getLabel());
            type.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

            row.getChildren().addAll(icon, title, type);
            card.getChildren().add(row);
        }
        return card;
    }

    private HBox infoRow(String key, String value) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("source-info-key");
        k.setMinWidth(110);

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
        k.setMinWidth(110);
        HBox row = new HBox(8, k, node);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private VBox buildPlaceholderCard(String title, String description, String phase) {
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("section-header");

        Label descLbl = new Label(description);
        descLbl.setWrapText(true);
        descLbl.getStyleClass().add("placeholder-desc");

        Label phaseLbl = new Label(phase);
        phaseLbl.getStyleClass().add("placeholder-phase");

        VBox card = new VBox(6, titleLbl, descLbl, phaseLbl);
        card.getStyleClass().add("card");
        return card;
    }
}