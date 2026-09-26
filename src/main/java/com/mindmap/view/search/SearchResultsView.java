package com.mindmap.view.search;

import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.SearchResults;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
import com.mindmap.view.knowledge.KnowledgeView;
import com.mindmap.view.source.SourcesView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Read-only search results page. Groups results by entity type.
 * Clicking a result navigates to the corresponding top-level view
 * and opens the item's detail page.
 */
public class SearchResultsView extends VBox {

    private final SearchResults results;

    public SearchResultsView(SearchResults results) {
        this.results = results;

        setSpacing(14);
        setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(
                buildHeader(),
                buildScrollableSections()
        );
    }

    // ------------------------------------------------------------- header

    private VBox buildHeader() {
        Label title = new Label("Search results");
        title.getStyleClass().add("view-header");

        String summary;
        if (results.isEmpty()) {
            summary = "No matches for \"" + results.getQuery() + "\".";
        } else {
            summary = results.getTotalCount() + " result"
                    + (results.getTotalCount() == 1 ? "" : "s")
                    + " for \"" + results.getQuery() + "\"";
        }
        Label subtitle = new Label(summary);
        subtitle.getStyleClass().add("view-subtitle");

        return new VBox(2, title, subtitle);
    }

    // ------------------------------------------------------------- body

    private ScrollPane buildScrollableSections() {
        VBox sections = new VBox(14);
        sections.setPadding(new Insets(0, 2, 4, 2));

        sections.getChildren().add(buildSection(
                "📚  Sources",
                results.getSources().size(),
                buildSourceSection()
        ));

        sections.getChildren().add(buildSection(
                "🧠  Knowledge",
                results.getKnowledge().size(),
                buildKnowledgeSection()
        ));

        sections.getChildren().add(buildSection(
                "🏷  Tags",
                results.getTags().size(),
                buildTagSection()
        ));

        if (results.isEmpty()) {
            Label empty = new Label("Try a different word, or check your spelling.");
            empty.getStyleClass().add("placeholder-desc");
            empty.setPadding(new Insets(20, 0, 0, 4));
            sections.getChildren().add(empty);
        }

        ScrollPane scroll = new ScrollPane(sections);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private VBox buildSection(String title, int count, VBox content) {
        Label header = new Label(title + "   (" + count + ")");
        header.getStyleClass().add("section-header");

        VBox section = new VBox(6, header, new Separator(), content);
        section.setPadding(new Insets(8, 4, 8, 4));
        if (count == 0) {
            Label none = new Label("No matches.");
            none.getStyleClass().add("placeholder-desc");
            section.getChildren().add(none);
        }
        return section;
    }

    // ------------------------------------------------------------- sections

    private VBox buildSourceSection() {
        VBox box = new VBox(4);
        for (Source s : results.getSources()) {
            HBox row = clickableRow(
                    s.getSourceType().getIcon(),
                    s.getTitle(),
                    s.getSourceType().getLabel()
                            + (s.getAuthorCreator() != null ? "  ·  " + s.getAuthorCreator() : "")
            );
            row.setOnMouseClicked(e -> SourcesView.navigateAndOpen(s.getId()));
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox buildKnowledgeSection() {
        VBox box = new VBox(4);
        for (KnowledgeItem k : results.getKnowledge()) {
            String meta = k.getItemType().getLabel()
                    + (k.getCategory() != null ? "  ·  " + k.getCategory() : "");
            if (results.matchedInTakeaway(k)) {
                meta += "   📝 takeaway";
            }
            HBox row = clickableRow(k.getItemType().getIcon(), k.getTitle(), meta);
            row.setOnMouseClicked(e -> KnowledgeView.navigateAndOpen(k.getId()));
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox buildTagSection() {
        VBox box = new VBox(4);
        for (Tag t : results.getTags()) {
            HBox row = clickableRow("🏷", t.getName(), "Tag");
            // Clicking a tag: navigate to Knowledge and filter by that tag name
            row.setOnMouseClicked(e -> {
                // Phase 7 keeps this simple: navigate to Knowledge.
                // Phase 8+ can add tag-based filtering.
                com.mindmap.util.SceneNavigator.getInstance()
                        .navigateTo(com.mindmap.util.SceneNavigator.KNOWLEDGE);
            });
            box.getChildren().add(row);
        }
        return box;
    }

    // ------------------------------------------------------------- rows

    private HBox clickableRow(String icon, String title, String meta) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("source-info-value");
        titleLbl.setStyle("-fx-font-size: 13.5px;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label metaLbl = new Label(meta);
        metaLbl.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

        HBox row = new HBox(10, iconLbl, titleLbl, metaLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;"));

        return row;
    }
}