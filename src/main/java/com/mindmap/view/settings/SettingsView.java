package com.mindmap.view.settings;

import com.mindmap.util.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Application settings. Phase 7 ships the Tags section;
 * other sections (theme, AI, backup) are placeholders for later phases.
 */
public class SettingsView extends VBox {

    public SettingsView() {
        setSpacing(14);
        setPadding(new Insets(4, 0, 0, 0));

        Label title = new Label("Settings");
        title.getStyleClass().add("view-header");

        Label subtitle = new Label("Manage your tags, theme, AI provider, and backups.");
        subtitle.getStyleClass().add("view-subtitle");

        ScrollPane scroll = new ScrollPane(buildSections());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), scroll);
    }

    private VBox buildSections() {
        VBox sections = new VBox(14);
        sections.setPadding(new Insets(0, 2, 4, 2));

        sections.getChildren().addAll(
                buildTagsSection(),
                buildPlaceholder("🎨  Appearance",
                        "Theme, font size, and accent color will be customizable here.",
                        "Phase 19 — UI Polishing"),
                buildPlaceholder("🤖  AI Provider",
                        "Set your AI provider API key and model. The key is stored securely "
                                + "and never committed to source control.",
                        "Phase 9 — AI Learning Extraction"),
                buildPlaceholder("💾  Backup & Restore",
                        "Export your MindMap data as a ZIP file or restore from a previous backup.",
                        "Phase 17 — Backup & Restore")
        );

        return sections;
    }

    private VBox buildTagsSection() {
        Label header = new Label("🏷  Tags");
        header.getStyleClass().add("section-header");

        Label desc = new Label("Rename, recolor, or delete tags. New tags are added automatically "
                + "when you type them into a source or knowledge form.");
        desc.setWrapText(true);
        desc.getStyleClass().add("placeholder-desc");

        Button manageBtn = new Button("Manage Tags…");
        manageBtn.getStyleClass().add("primary-button");
        manageBtn.setOnAction(e -> new TagsManagerDialog().showAndWait());

        HBox buttonRow = new HBox(manageBtn);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setPadding(new Insets(4, 0, 0, 0));

        VBox card = new VBox(8, header, desc, new Separator(), buttonRow);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildPlaceholder(String title, String description, String phase) {
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