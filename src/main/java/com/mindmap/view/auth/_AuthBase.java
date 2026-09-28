package com.mindmap.view.auth;

import com.mindmap.util.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Base class for the Login and Register screens.
 * Provides: gradient background, branding panel, form card, theme toggle,
 * styled fields and buttons.
 */
abstract class _AuthBase extends StackPane {

    protected _AuthBase() {
        getStyleClass().add("auth-root");
        getChildren().add(buildThemeToggleCorner());
    }

    /** Branded, centered card with title + subtitle. Add form fields to the returned VBox. */
    protected VBox buildCard(String title, String subtitle) {
        Label icon = new Label("🧠");
        icon.getStyleClass().add("auth-brand-icon");

        Label brand = new Label("MindMap");
        brand.getStyleClass().add("auth-brand-title");

        Label tagline = new Label("Personal Knowledge & Learning Discovery Platform");
        tagline.getStyleClass().add("auth-brand-subtitle");

        VBox brandBox = new VBox(4, icon, brand, tagline);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(0, 0, 22, 0));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("auth-card-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("auth-card-subtitle");

        VBox card = new VBox(14, brandBox, titleLabel, subtitleLabel);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(440);
        card.setPrefWidth(440);

        return card;
    }

    /** Label + field stacked vertically. */
    protected VBox field(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("auth-field-label");
        field.getStyleClass().add("auth-text-field");
        field.setPrefHeight(40);

        VBox box = new VBox(6, label, field);
        return box;
    }

    /** Label + password field stacked vertically. */
    protected VBox passwordField(String labelText, PasswordField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("auth-field-label");
        field.getStyleClass().add("auth-text-field");
        field.setPrefHeight(40);

        VBox box = new VBox(6, label, field);
        return box;
    }

    /** Full-width primary action button. */
    protected Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("auth-primary-button");
        btn.setPrefHeight(42);
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    /** Inline error label (empty by default). */
    protected Label errorLabel() {
        Label lbl = new Label();
        lbl.getStyleClass().add("auth-error-label");
        lbl.setWrapText(true);
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    /** Sets an error message on the label and makes it visible. */
    protected void showError(Label errorLabel, String message) {
        if (message == null || message.isBlank()) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText("⚠  " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /** Centered clickable link. */
    protected Hyperlink link(String text) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("auth-link");
        return link;
    }

    /** Row containing e.g. "New here?  [Create an account]". */
    protected HBox linkRow(String prefix, Hyperlink link) {
        Label lbl = new Label(prefix);
        lbl.getStyleClass().add("auth-link-prefix");
        HBox row = new HBox(4, lbl, link);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

    // ------------------------------------------------------------ theme toggle

    private HBox buildThemeToggleCorner() {
        Label themeToggle = new Label("🌙");
        themeToggle.getStyleClass().add("auth-theme-toggle");
        themeToggle.setOnMouseClicked(e -> {
            AppContext.getInstance().toggleTheme();
            themeToggle.setText(AppContext.getInstance().isDarkTheme() ? "☀" : "🌙");
        });
        // Sync initial state
        themeToggle.setText(AppContext.getInstance().isDarkTheme() ? "☀" : "🌙");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(spacer, themeToggle);
        row.setAlignment(Pos.TOP_RIGHT);
        row.setPadding(new Insets(18, 20, 0, 0));
        row.setPickOnBounds(false);
        return row;
    }
}