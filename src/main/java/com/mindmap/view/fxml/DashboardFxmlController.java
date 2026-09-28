package com.mindmap.view.fxml;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.service.AnalyticsService;
import com.mindmap.service.KnowledgeService;
import com.mindmap.util.exceptions.ServiceException;
import com.mindmap.view.knowledge.KnowledgeView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * FXML controller for dashboard.fxml.
 * Loads real stats from AnalyticsService.
 */
public class DashboardFxmlController {

    private final AnalyticsService analytics = ServiceRegistry.analyticsService();
    private final KnowledgeService knowledge = ServiceRegistry.knowledgeService();

    @FXML private GridPane statsGrid;
    @FXML private VBox recentBox;
    @FXML private Label statusMessage;

    @FXML
    private void initialize() {
        statusMessage.setText("Loading…");

        new Thread(() -> {
            try {
                AnalyticsService.DashboardStats stats = analytics.getDashboardStats();
                List<KnowledgeItem> recent = knowledge.getRecent(6);

                Platform.runLater(() -> {
                    buildStats(stats);
                    buildRecent(recent);
                    statusMessage.setText("Live data");
                });
            } catch (ServiceException e) {
                Platform.runLater(() -> statusMessage.setText("Could not load: " + e.getMessage()));
            }
        }, "fxml-dashboard-loader").start();
    }

    private void buildStats(AnalyticsService.DashboardStats s) {
        statsGrid.getChildren().clear();

        addTile(0, 0, "📚", "Sources",       String.valueOf(s.totalSources()),     "#3498DB");
        addTile(1, 0, "🧠", "Knowledge",     String.valueOf(s.totalKnowledge()),   "#9B59B6");
        addTile(2, 0, "🔗", "Connections",   String.valueOf(s.totalConnections()), "#27AE60");
        addTile(3, 0, "🔄", "Reviews Due",   String.valueOf(s.reviewsDue()),       "#F39C12");

        addTile(0, 1, "✅", "Reviewed Today",String.valueOf(s.reviewedToday()),    "#1ABC9C");
        addTile(1, 1, "🔥", "Current Streak",s.currentStreak() + "d",              "#E74C3C");
        addTile(2, 1, "🎴", "Flashcards",    String.valueOf(s.totalFlashcards()),  "#795548");
        addTile(3, 1, "📝", "Quiz Questions",String.valueOf(s.totalQuizQuestions()),"#607D8B");
    }

    private void addTile(int col, int row, String icon, String label, String value, String color) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

        VBox text = new VBox(0, valueLbl, labelLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox content = new HBox(10, iconLbl, text);
        content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox tile = new VBox(content);
        tile.setPadding(new Insets(14, 16, 14, 16));
        tile.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, -fx-shadow, 6, 0, 0, 2);");
        tile.setPrefWidth(180);
        tile.setPrefHeight(84);

        statsGrid.add(tile, col, row);
    }

    private void buildRecent(List<KnowledgeItem> recent) {
        recentBox.getChildren().clear();
        if (recent.isEmpty()) {
            Label empty = new Label("No knowledge items yet. Add some from the Knowledge page.");
            empty.getStyleClass().add("placeholder-desc");
            recentBox.getChildren().add(empty);
            return;
        }
        for (KnowledgeItem k : recent) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 8; "
                    + "-fx-cursor: hand;");

            Label icon = new Label(k.getItemType().getIcon());
            icon.setStyle("-fx-font-size: 18px;");

            Label title = new Label(k.getTitle());
            title.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13px;");
            HBox.setHgrow(title, Priority.ALWAYS);

            row.getChildren().addAll(icon, title);
            row.setOnMouseClicked(e -> KnowledgeView.navigateAndOpen(k.getId()));
            recentBox.getChildren().add(row);
        }
    }
}