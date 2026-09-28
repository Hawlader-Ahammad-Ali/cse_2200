package com.mindmap.view.settings;

import com.mindmap.config.UserPreferences;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Application settings.
 *
 * <p>Sections:
 * <ul>
 *   <li>Appearance — theme, accent, font size</li>
 *   <li>AI Provider — API key, model, base URL</li>
 *   <li>Backup &amp; Restore — export / import ZIP</li>
 *   <li>Tags — manage tag list</li>
 *   <li>About — version and data directory</li>
 * </ul>
 * </p>
 */
public class SettingsView extends VBox {

    public SettingsView() {
        setSpacing(14);
        setPadding(new Insets(4, 0, 0, 0));

        Label title = new Label("Settings");
        title.getStyleClass().add("view-header");

        Label subtitle = new Label("Manage your theme, AI provider, backups, and tags.");
        subtitle.getStyleClass().add("view-subtitle");

        // Load saved preferences once for the Appearance panel
        UserPreferences prefs = UserPreferences.load();

        ScrollPane scroll = new ScrollPane(buildSections(prefs));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(new VBox(2, title, subtitle), scroll);
    }

    // =============================================================
    // Sections
    // =============================================================

    private VBox buildSections(UserPreferences prefs) {
        VBox sections = new VBox(14);
        sections.setPadding(new Insets(0, 2, 4, 2));

        sections.getChildren().addAll(
                wrapInCard(new AppearancePanel(prefs)),
                wrapInCard(new AIProviderPanel()),
                wrapInCard(new BackupPanel()),
                wrapInCard(buildTagsSection()),
                wrapInCard(buildAboutSection())
        );

        return sections;
    }

    // ------------------------------------------------------------- Tags

    private VBox buildTagsSection() {
        Label header = new Label("🏷  Tags");
        header.getStyleClass().add("section-header");

        Label desc = new Label("Rename, recolor, or delete tags. New tags are added "
                + "automatically when you type them into a source or knowledge form.");
        desc.setWrapText(true);
        desc.getStyleClass().add("placeholder-desc");

        Button manageBtn = new Button("Manage Tags…");
        manageBtn.getStyleClass().add("primary-button");
        manageBtn.setOnAction(e -> new TagsManagerDialog().showAndWait());

        HBox buttonRow = new HBox(manageBtn);
        buttonRow.setPadding(new Insets(4, 0, 0, 0));

        return new VBox(8, header, desc, new Separator(), buttonRow);
    }

    // ------------------------------------------------------------- About

    private VBox buildAboutSection() {
        Label header = new Label("ℹ  About MindMap");
        header.getStyleClass().add("section-header");

        Label tagline = new Label("\"Learn from anything. Connect everything. Remember what matters.\"");
        tagline.setWrapText(true);
        tagline.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-style: italic; "
                + "-fx-font-size: 13px;");

        Label version = new Label("Version " + com.mindmap.config.AppConfig.APP_VERSION);
        version.getStyleClass().add("view-subtitle");

        Label path = new Label("Data directory: " + com.mindmap.util.AppPaths.getAppDataDir());
        path.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px;");
        path.setWrapText(true);

        return new VBox(6, header, tagline, version, new Separator(), path);
    }

    // =============================================================
    // Helpers
    // =============================================================

    private VBox wrapInCard(Node content) {
        VBox card = new VBox(content);
        card.getStyleClass().add("card");
        return card;
    }
}