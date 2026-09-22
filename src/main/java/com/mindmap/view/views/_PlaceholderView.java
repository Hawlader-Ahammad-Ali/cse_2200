package com.mindmap.view.views;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Reusable placeholder card shown until a view's real UI is built.
 * Package-private helper — not part of the public API.
 */
class _PlaceholderView extends StackPane {

    protected _PlaceholderView(String icon,
                               String title,
                               String description,
                               String phaseLabel) {

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("placeholder-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("placeholder-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("placeholder-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(440);

        Label phase = new Label(phaseLabel);
        phase.getStyleClass().add("placeholder-phase");

        VBox card = new VBox(12, iconLabel, titleLabel, descLabel, phase);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("placeholder-card");
        card.setMaxWidth(560);
        card.setMaxHeight(320);

        setAlignment(Pos.CENTER);
        getChildren().add(card);
    }
}