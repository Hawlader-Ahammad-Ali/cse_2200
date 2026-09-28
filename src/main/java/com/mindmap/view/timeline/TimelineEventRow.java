package com.mindmap.view.timeline;

import com.mindmap.model.TimelineEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * One timeline row: [icon] [title / subtitle] [timestamp]
 * Not a control — just a factory for a styled HBox.
 */
class TimelineEventRow {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private TimelineEventRow() { }

    static HBox build(TimelineEvent e, Runnable onClick) {
        Label icon = new Label(e.getKind().getIcon());
        icon.setStyle("-fx-font-size: 20px;");
        icon.setMinWidth(32);

        Label kind = new Label(e.getKind().getLabel());
        kind.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 10.5px; "
                + "-fx-font-weight: bold;");

        Label title = new Label(safe(e.getTitle()));
        title.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13.5px; "
                + "-fx-font-weight: bold;");
        title.setWrapText(true);

        Label subtitle = new Label(safe(e.getSubtitle()));
        subtitle.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");
        subtitle.setWrapText(true);

        VBox text = new VBox(2, kind, title, subtitle);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label time = new Label(e.getTimestamp() == null
                ? ""
                : e.getTimestamp().format(TIME_FMT));
        time.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");
        time.setMinWidth(48);
        time.setAlignment(Pos.TOP_RIGHT);

        HBox row = new HBox(10, icon, text, time);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 8; "
                + "-fx-cursor: hand;");

        row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(ev -> row.setStyle(
                "-fx-background-color: -fx-bg-primary; -fx-background-radius: 8; -fx-cursor: hand;"));

        if (onClick != null) {
            row.setOnMouseClicked(ev -> onClick.run());
        }

        return row;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}