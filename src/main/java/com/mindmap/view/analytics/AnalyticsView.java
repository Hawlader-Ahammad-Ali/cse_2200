package com.mindmap.view.analytics;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.service.AnalyticsService;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import com.mindmap.export.ExportButtonHelper;
/**
 * Deep-dive analytics page.
 */
public class AnalyticsView extends VBox {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsView.class);

    private final AnalyticsService analytics = ServiceRegistry.analyticsService();

    private final VBox content = new VBox(14);

    public AnalyticsView() {
        setSpacing(12);
        setPadding(new Insets(4, 0, 0, 0));

        Label title = new Label("Analytics");
        title.getStyleClass().add("view-header");

        Label subtitle = new Label("Insights derived from your actual learning data.");
        subtitle.getStyleClass().add("view-subtitle");

        Button refreshBtn = new Button("↻  Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = ExportButtonHelper.reportExportButton();

        HBox header = new HBox(10, new VBox(2, title, subtitle), spacer, exportBtn, refreshBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, scroll);

        refresh();
    }

    // ------------------------------------------------------------- refresh

    private void refresh() {
        content.getChildren().clear();
        content.getChildren().add(new Label("Loading analytics…"));

        new Thread(() -> {
            try {
                Map<String, Integer> categories = analytics.getCategoryDistribution();
                List<String> signals = analytics.getInterestSignals(5);
                List<AnalyticsService.TopicAccuracy> weak = analytics.getWeakTopics(5);
                List<AnalyticsService.TopicAccuracy> strong = analytics.getStrongTopics(5);
                List<AnalyticsService.CrossSourceConcept> cross = analytics.getTopCrossSourceConcepts(5);
                List<AnalyticsService.MonthlyGrowth> growth = analytics.getMonthlyGrowth(6);

                Platform.runLater(() -> render(categories, signals, weak, strong, cross, growth));

            } catch (ServiceException e) {
                Platform.runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().add(new Label("Could not load analytics: " + e.getMessage()));
                });
            }
        }, "analytics-loader").start();
    }

    private void render(Map<String, Integer> categories,
                        List<String> signals,
                        List<AnalyticsService.TopicAccuracy> weak,
                        List<AnalyticsService.TopicAccuracy> strong,
                        List<AnalyticsService.CrossSourceConcept> cross,
                        List<AnalyticsService.MonthlyGrowth> growth) {

        content.getChildren().clear();

        content.getChildren().add(buildTopCategoriesChart(categories));
        content.getChildren().add(buildInterestSignals(signals));
        content.getChildren().add(buildTwoColumn(weak, strong));
        content.getChildren().add(buildCrossSourceCard(cross));
        content.getChildren().add(buildGrowthTable(growth));
    }

    // ------------------------------------------------------------- sections

    private VBox buildTopCategoriesChart(Map<String, Integer> categories) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Top Learning Categories");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int n = 0;
        for (Map.Entry<String, Integer> e : categories.entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            if (++n >= 8) break;
        }
        chart.getData().add(series);

        VBox card = new VBox(chart);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: -fx-bg-primary; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(gaussian, -fx-shadow, 6, 0, 0, 2);");
        return card;
    }

    private VBox buildInterestSignals(List<String> signals) {
        Label header = new Label("🎯  What Am I Learning?");
        header.getStyleClass().add("section-header");

        Label note = new Label("Derived from your top categories. This is not a psychological "
                + "profile — it simply reflects what's in your knowledge base right now.");
        note.setWrapText(true);
        note.getStyleClass().add("placeholder-desc");

        VBox card = new VBox(8, header, note, new Separator());

        if (signals.isEmpty()) {
            Label empty = new Label("Add and categorize knowledge items to see your interests.");
            empty.getStyleClass().add("placeholder-desc");
            card.getChildren().add(empty);
        } else {
            int rank = 1;
            for (String s : signals) {
                Label row = new Label(rank + ".   " + s);
                row.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 14px; "
                        + "-fx-padding: 4 0 4 0;");
                card.getChildren().add(row);
                rank++;
            }
        }

        card.getStyleClass().add("card");
        return card;
    }

    private HBox buildTwoColumn(List<AnalyticsService.TopicAccuracy> weak,
                                List<AnalyticsService.TopicAccuracy> strong) {
        VBox weakCard = buildTopicCard("📉  Weak Topics",
                "Concepts you've gotten wrong more often than right.",
                weak, true);

        VBox strongCard = buildTopicCard("💪  Strong Topics",
                "Concepts you consistently recall correctly.",
                strong, false);

        HBox.setHgrow(weakCard, Priority.ALWAYS);
        HBox.setHgrow(strongCard, Priority.ALWAYS);

        HBox row = new HBox(14, weakCard, strongCard);
        return row;
    }

    private VBox buildTopicCard(String title, String hint,
                                List<AnalyticsService.TopicAccuracy> topics,
                                boolean isWeak) {
        Label header = new Label(title);
        header.getStyleClass().add("section-header");

        Label hintLbl = new Label(hint);
        hintLbl.setWrapText(true);
        hintLbl.getStyleClass().add("placeholder-desc");

        VBox card = new VBox(8, header, hintLbl, new Separator());

        if (topics.isEmpty()) {
            Label empty = new Label("Not enough data yet — take a few quizzes to see this.");
            empty.getStyleClass().add("placeholder-desc");
            empty.setWrapText(true);
            card.getChildren().add(empty);
        } else {
            for (AnalyticsService.TopicAccuracy t : topics) {
                int pct = (int) Math.round(t.getAccuracy() * 100);
                String color = isWeak ? "#E74C3C" : "#27AE60";

                Label titleLbl = new Label(t.label());
                titleLbl.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13px;");
                titleLbl.setWrapText(true);
                HBox.setHgrow(titleLbl, Priority.ALWAYS);

                Label scoreLbl = new Label(pct + "%  (" + t.correct() + "/" + t.attempts() + ")");
                scoreLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; "
                        + "-fx-font-size: 12px;");

                HBox row = new HBox(8, titleLbl, scoreLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));

                card.getChildren().add(row);
            }
        }

        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildCrossSourceCard(List<AnalyticsService.CrossSourceConcept> cross) {
        Label header = new Label("🔀  Concepts Learned Across Multiple Sources");
        header.getStyleClass().add("section-header");

        Label hint = new Label("Ideas that appear in more than one of your sources — often "
                + "the most valuable connections to strengthen.");
        hint.setWrapText(true);
        hint.getStyleClass().add("placeholder-desc");

        VBox card = new VBox(8, header, hint, new Separator());

        if (cross.isEmpty()) {
            Label empty = new Label("Link knowledge items to multiple sources to see this.");
            empty.getStyleClass().add("placeholder-desc");
            card.getChildren().add(empty);
        } else {
            for (AnalyticsService.CrossSourceConcept c : cross) {
                Label titleLbl = new Label(c.title());
                titleLbl.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 13px;");
                titleLbl.setWrapText(true);
                HBox.setHgrow(titleLbl, Priority.ALWAYS);

                Label badge = new Label(c.sourceCount() + " sources");
                badge.setStyle("-fx-background-color: -fx-accent-soft; -fx-text-fill: -fx-accent-hover; "
                        + "-fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px; "
                        + "-fx-font-weight: bold;");

                HBox row = new HBox(8, titleLbl, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));
                card.getChildren().add(row);
            }
        }

        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildGrowthTable(List<AnalyticsService.MonthlyGrowth> growth) {
        Label header = new Label("📈  Monthly Growth (last 6 months)");
        header.getStyleClass().add("section-header");

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 4, 0));

        grid.add(bold("Month"), 0, 0);
        grid.add(bold("Sources"), 1, 0);
        grid.add(bold("Knowledge"), 2, 0);
        grid.add(bold("Reviews"), 3, 0);

        int row = 1;
        for (AnalyticsService.MonthlyGrowth g : growth) {
            grid.add(new Label(AnalyticsService.prettyMonth(g.month())), 0, row);
            grid.add(new Label(String.valueOf(g.sources())), 1, row);
            grid.add(new Label(String.valueOf(g.knowledge())), 2, row);
            grid.add(new Label(String.valueOf(g.reviews())), 3, row);
            row++;
        }

        VBox card = new VBox(8, header, grid);
        card.getStyleClass().add("card");
        return card;
    }

    private Label bold(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");
        return l;
    }
}