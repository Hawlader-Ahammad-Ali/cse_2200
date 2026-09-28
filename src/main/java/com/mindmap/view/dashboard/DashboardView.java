package com.mindmap.view.dashboard;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.service.AnalyticsService;
import com.mindmap.service.KnowledgeService;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import com.mindmap.view.knowledge.KnowledgeView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Live dashboard: real stats + three charts + recent knowledge items.
 * All numbers come from {@link AnalyticsService}.
 */
public class DashboardView extends VBox {

    private static final Logger log = LoggerFactory.getLogger(DashboardView.class);

    private final AnalyticsService analytics = ServiceRegistry.analyticsService();
    private final KnowledgeService knowledge = ServiceRegistry.knowledgeService();

    private final GridPane statsGrid = new GridPane();
    private final VBox chartsBox = new VBox(14);
    private final VBox recentBox = new VBox(6);

    public DashboardView() {
        setSpacing(14);
        setPadding(new Insets(4, 0, 0, 0));

        Label title = new Label("Dashboard");
        title.getStyleClass().add("view-header");

        Button refreshBtn = new Button("↻  Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, title, spacer, refreshBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(new VBox(14, statsGrid, chartsBox, recentBox));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, scroll);

        refresh();
    }

    // ------------------------------------------------------------- refresh

    private void refresh() {
        // Show a quick loading state
        statsGrid.getChildren().clear();
        chartsBox.getChildren().clear();
        recentBox.getChildren().clear();

        // Run on a worker thread since we hit the DB several times
        new Thread(() -> {
            try {
                AnalyticsService.DashboardStats stats = analytics.getDashboardStats();
                Map<String, Integer> categories = analytics.getCategoryDistribution();
                Map<YearMonth, Integer> knowledgeOverTime = analytics.getKnowledgeOverTime();
                Map<LocalDate, Integer> activity = analytics.getActivityLast30Days();
                List<KnowledgeItem> recent = knowledge.getRecent(8);

                Platform.runLater(() -> renderAll(stats, categories,
                        knowledgeOverTime, activity, recent));

            } catch (ServiceException e) {
                Platform.runLater(() -> Dialogs.error("Could not load dashboard", e.getMessage()));
            }
        }, "dashboard-loader").start();
    }

    private void renderAll(AnalyticsService.DashboardStats stats,
                           Map<String, Integer> categories,
                           Map<YearMonth, Integer> knowledgeOverTime,
                           Map<LocalDate, Integer> activity,
                           List<KnowledgeItem> recent) {

        buildStatsGrid(stats);
        buildCharts(categories, knowledgeOverTime, activity);
        buildRecent(recent);
    }

    // ------------------------------------------------------------- stat tiles

    private void buildStatsGrid(AnalyticsService.DashboardStats s) {
        statsGrid.setHgap(14);
        statsGrid.setVgap(14);
        statsGrid.getChildren().clear();

        int col = 0;
        addStatTile(statsGrid, col++, "📚", "Sources",       String.valueOf(s.totalSources()),     "#3498DB");
        addStatTile(statsGrid, col++, "🧠", "Knowledge",     String.valueOf(s.totalKnowledge()),   "#9B59B6");
        addStatTile(statsGrid, col++, "🔗", "Connections",   String.valueOf(s.totalConnections()), "#27AE60");
        addStatTile(statsGrid, col++, "🔄", "Reviews Due",   String.valueOf(s.reviewsDue()),       "#F39C12");

        col = 0;
        addStatTile(statsGrid, col++, "✅", "Reviewed Today", String.valueOf(s.reviewedToday()),   "#1ABC9C");
        addStatTile(statsGrid, col++, "🔥", "Current Streak", s.currentStreak() + "d",             "#E74C3C");
        addStatTile(statsGrid, col++, "🎴", "Flashcards",     String.valueOf(s.totalFlashcards()), "#795548");
        addStatTile(statsGrid, col++, "📝", "Quiz Questions", String.valueOf(s.totalQuizQuestions()), "#607D8B");
    }

    private void addStatTile(GridPane grid, int col, String icon, String label, String value, String color) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

        VBox text = new VBox(0, valueLbl, labelLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox content = new HBox(10, iconLbl, text);
        content.setAlignment(Pos.CENTER_LEFT);

        VBox tile = new VBox(content);
        tile.setPadding(new Insets(14, 16, 14, 16));
        tile.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, -fx-shadow, 6, 0, 0, 2);");
        tile.setPrefWidth(180);
        tile.setPrefHeight(84);

        grid.add(tile, col, GridPane.getRowIndex(tile) == null ? 0 : GridPane.getRowIndex(tile));
    }

    // ------------------------------------------------------------- charts

    private void buildCharts(Map<String, Integer> categories,
                             Map<YearMonth, Integer> growth,
                             Map<LocalDate, Integer> activity) {

        chartsBox.getChildren().clear();

        // Row 1: line chart (growth) | pie chart (categories)
        VBox lineCard = buildLineCard(growth);
        VBox pieCard = buildPieCard(categories);

        HBox row1 = new HBox(14, lineCard, pieCard);
        HBox.setHgrow(lineCard, Priority.ALWAYS);
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        // Row 2: bar chart (activity)
        VBox barCard = buildActivityCard(activity);

        chartsBox.getChildren().addAll(row1, barCard);
    }

    private VBox buildLineCard(Map<YearMonth, Integer> growth) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Knowledge Growth (last 12 months)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<YearMonth, Integer> e : growth.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    AnalyticsService.prettyMonth(e.getKey()),
                    e.getValue()));
        }
        chart.getData().add(series);

        VBox card = new VBox(chart);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, -fx-shadow, 6, 0, 0, 2);");
        return card;
    }

    private VBox buildPieCard(Map<String, Integer> categories) {
        PieChart pie = new PieChart();
        pie.setTitle("Category Distribution");
        pie.setLegendVisible(true);
        pie.setAnimated(false);
        pie.setPrefHeight(260);

        int count = 0;
        for (Map.Entry<String, Integer> e : categories.entrySet()) {
            pie.getData().add(new PieChart.Data(e.getKey(), e.getValue()));
            if (++count >= 6) break;   // top 6
        }

        if (pie.getData().isEmpty()) {
            pie.setTitle("Category Distribution (no data yet)");
        }

        VBox card = new VBox(pie);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, -fx-shadow, 6, 0, 0, 2);");
        return card;
    }

    private VBox buildActivityCard(Map<LocalDate, Integer> activity) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Daily Activity (last 30 days)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(240);
        chart.setCategoryGap(1);
        chart.setBarGap(0);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<LocalDate, Integer> e : activity.entrySet()) {
            // Short label: day number only, so bars don't crowd
            String label = String.valueOf(e.getKey().getDayOfMonth());
            series.getData().add(new XYChart.Data<>(label, e.getValue()));
        }
        chart.getData().add(series);

        VBox card = new VBox(chart);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, -fx-shadow, 6, 0, 0, 2);");
        return card;
    }

    // ------------------------------------------------------------- recent

    private void buildRecent(List<KnowledgeItem> recent) {
        recentBox.getChildren().clear();

        Label header = new Label("Recently Added Knowledge");
        header.getStyleClass().add("section-header");

        recentBox.getChildren().add(header);

        if (recent.isEmpty()) {
            Label empty = new Label("No knowledge items yet. Add some to see them here.");
            empty.getStyleClass().add("placeholder-desc");
            recentBox.getChildren().add(empty);
            return;
        }

        for (KnowledgeItem k : recent) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 8; "
                    + "-fx-cursor: hand;");

            Label icon = new Label(k.getItemType().getIcon());
            icon.setStyle("-fx-font-size: 18px;");

            Label title = new Label(k.getTitle());
            title.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13px;");
            HBox.setHgrow(title, Priority.ALWAYS);

            Label cat = new Label(k.getCategory() == null ? "" : k.getCategory());
            cat.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

            row.getChildren().addAll(icon, title, cat);

            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-background-color: -fx-bg-tertiary; -fx-background-radius: 8; -fx-cursor: hand;"));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-background-color: -fx-bg-primary; -fx-background-radius: 8; -fx-cursor: hand;"));
            row.setOnMouseClicked(e -> KnowledgeView.navigateAndOpen(k.getId()));

            recentBox.getChildren().add(row);
        }
    }
}