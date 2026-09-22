package com.mindmap.view.source;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
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
 * Read-only detail view for a single source. The Related Knowledge section
 * is now live: it loads linked knowledge items from
 * {@code SourceKnowledgeDAO} and lets the user click through to them.
 */
public class SourceDetailView extends VBox {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final Source source;
    private final Consumer<Source> onEdit;
    private final Runnable onDelete;
    private final Runnable onBack;
    private final Consumer<KnowledgeItem> onOpenKnowledge;

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

        Button editBtn = new Button("✎  Edit");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> onEdit.accept(source));

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
                buildPlaceholderCard("🤖  AI Suggested Learning",
                        "Topics, concepts, and skills suggested by AI will appear here. "
                                + "You will review and confirm them before they enter your knowledge base.",
                        "Phase 9 — AI Learning Extraction"),
                buildRelatedKnowledgeCard(),
                buildPlaceholderCard("📝  My Takeaways",
                        "Your personal notes and lessons learned from this source will appear here.",
                        "Phase 10 — Personal Takeaways")
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

    /** Live Related Knowledge section. */
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
            Label error = new Label("Could not load related knowledge.");
            error.getStyleClass().add("placeholder-desc");
            card.getChildren().add(error);
            return card;
        }

        if (items.isEmpty()) {
            Label empty = new Label("No knowledge items linked to this source yet. "
                    + "Add some from the Knowledge page, or use AI analysis (Phase 9).");
            empty.getStyleClass().add("placeholder-desc");
            empty.setWrapText(true);
            card.getChildren().add(empty);
            return card;
        }

        for (KnowledgeItem k : items) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));
            row.setStyle("-fx-cursor: hand;");

            Label icon = new Label(k.getItemType().getIcon());
            icon.setStyle("-fx-font-size: 18px;");

            Label title = new Label(k.getTitle());
            title.getStyleClass().add("source-info-value");
            HBox.setHgrow(title, Priority.ALWAYS);

            Label type = new Label(k.getItemType().getLabel());
            type.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

            row.getChildren().addAll(icon, title, type);

            row.setOnMouseClicked(e -> {
                if (onOpenKnowledge != null) onOpenKnowledge.accept(k);
            });
            // Hover effect
            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-cursor: hand; -fx-background-color: -fx-bg-tertiary; -fx-background-radius: 6;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand;"));

            card.getChildren().add(row);
        }
        return card;
    }

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