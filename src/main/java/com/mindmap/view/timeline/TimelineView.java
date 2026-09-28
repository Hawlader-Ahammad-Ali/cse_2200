package com.mindmap.view.timeline;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.TimelineEvent;
import com.mindmap.service.TimelineService;
import com.mindmap.util.Dialogs;
import com.mindmap.util.SceneNavigator;
import com.mindmap.util.exceptions.ServiceException;
import com.mindmap.view.knowledge.KnowledgeView;
import com.mindmap.view.source.SourcesView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Timeline view — chronological feed of everything the user has done.
 * Grouped by month. Filters: from date, to date, event kind.
 */
public class TimelineView extends VBox {

    private static final Logger log = LoggerFactory.getLogger(TimelineView.class);

    private final TimelineService service = ServiceRegistry.timelineService();

    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker   = new DatePicker();
    private final ComboBox<String> kindFilter = new ComboBox<>();
    private final Label countLabel = new Label();

    private final VBox feed = new VBox(6);

    public TimelineView() {
        setSpacing(12);
        setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(buildHeader(), buildFilters(), buildScroll());

        refresh();
    }

    // ------------------------------------------------------------- header

    private VBox buildHeader() {
        Label title = new Label("Timeline");
        title.getStyleClass().add("view-header");

        countLabel.getStyleClass().add("view-subtitle");
        countLabel.setPadding(new Insets(4, 0, 0, 0));

        return new VBox(2, title, countLabel);
    }

    // ------------------------------------------------------------- filters

    private HBox buildFilters() {
        fromPicker.setPromptText("From date");
        fromPicker.setOnAction(e -> refresh());

        toPicker.setPromptText("To date");
        toPicker.setOnAction(e -> refresh());

        kindFilter.getItems().add("All events");
        for (TimelineEvent.Kind k : TimelineEvent.Kind.values()) {
            kindFilter.getItems().add(k.getLabel());
        }
        kindFilter.setValue("All events");
        kindFilter.getStyleClass().add("source-filter");
        kindFilter.setOnAction(e -> refresh());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary-button");
        clearBtn.setOnAction(e -> {
            fromPicker.setValue(null);
            toPicker.setValue(null);
            kindFilter.setValue("All events");
            refresh();
        });

        // Quick range buttons
        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("secondary-button");
        todayBtn.setOnAction(e -> {
            fromPicker.setValue(LocalDate.now());
            toPicker.setValue(LocalDate.now());
            refresh();
        });

        Button weekBtn = new Button("This week");
        weekBtn.getStyleClass().add("secondary-button");
        weekBtn.setOnAction(e -> {
            fromPicker.setValue(LocalDate.now().minusDays(7));
            toPicker.setValue(LocalDate.now());
            refresh();
        });

        Button monthBtn = new Button("This month");
        monthBtn.getStyleClass().add("secondary-button");
        monthBtn.setOnAction(e -> {
            fromPicker.setValue(LocalDate.now().withDayOfMonth(1));
            toPicker.setValue(LocalDate.now());
            refresh();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8,
                new Label("Range:"), fromPicker, toPicker, kindFilter,
                todayBtn, weekBtn, monthBtn, spacer, clearBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 4, 0));
        return row;
    }

    // ------------------------------------------------------------- feed

    private ScrollPane buildScroll() {
        feed.setPadding(new Insets(4, 2, 4, 2));

        ScrollPane scroll = new ScrollPane(feed);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void refresh() {
        LocalDate from = fromPicker.getValue();
        LocalDate to   = toPicker.getValue();
        TimelineEvent.Kind kind = kindFromFilter();

        List<TimelineEvent> events;
        try {
            events = service.getTimeline(from, to, kind);
        } catch (ServiceException e) {
            Dialogs.error("Could not load timeline", e.getMessage());
            return;
        }

        feed.getChildren().clear();

        countLabel.setText(events.size() + " event" + (events.size() == 1 ? "" : "s")
                + (from != null || to != null || kind != null ? " (filtered)" : ""));

        if (events.isEmpty()) {
            Label empty = new Label("No events in this range. "
                    + "Try widening the date filters or add some sources and knowledge.");
            empty.getStyleClass().add("placeholder-desc");
            empty.setWrapText(true);
            empty.setPadding(new Insets(20, 4, 4, 4));
            feed.getChildren().add(empty);
            return;
        }

        // Group by month
        Map<YearMonth, List<TimelineEvent>> grouped = new LinkedHashMap<>();
        for (TimelineEvent e : events) {
            if (e.getTimestamp() == null) continue;
            YearMonth ym = YearMonth.from(e.getTimestamp());
            grouped.computeIfAbsent(ym, k -> new ArrayList<>()).add(e);
        }

        for (Map.Entry<YearMonth, List<TimelineEvent>> entry : grouped.entrySet()) {
            feed.getChildren().add(buildMonthHeader(entry.getKey(), entry.getValue().size()));
            for (TimelineEvent e : entry.getValue()) {
                feed.getChildren().add(TimelineEventRow.build(e, () -> handleClick(e)));
            }
        }
    }

    private VBox buildMonthHeader(YearMonth ym, int count) {
        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        Label month = new Label(monthName + " " + ym.getYear());
        month.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; "
                + "-fx-text-fill: -fx-text-primary;");

        Label n = new Label(count + " event" + (count == 1 ? "" : "s"));
        n.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, month, n, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 4, 4, 4));

        return new VBox(row);
    }

    private TimelineEvent.Kind kindFromFilter() {
        String val = kindFilter.getValue();
        if (val == null || "All events".equals(val)) return null;
        for (TimelineEvent.Kind k : TimelineEvent.Kind.values()) {
            if (k.getLabel().equals(val)) return k;
        }
        return null;
    }

    private void handleClick(TimelineEvent e) {
        try {
            if (e.getKnowledgeItemId() != null) {
                KnowledgeView.navigateAndOpen(e.getKnowledgeItemId());
            } else if (e.getSourceId() != null) {
                SourcesView.navigateAndOpen(e.getSourceId());
            } else if (e.getFlashcardId() != null) {
                SceneNavigator.getInstance().navigateTo(SceneNavigator.FLASHCARDS);
            } else if (e.getKind() == TimelineEvent.Kind.REVIEW_COMPLETED) {
                SceneNavigator.getInstance().navigateTo(SceneNavigator.REVIEW);
            } else if (e.getKind() == TimelineEvent.Kind.QUIZ_COMPLETED) {
                SceneNavigator.getInstance().navigateTo(SceneNavigator.QUIZ);
            }
        } catch (Exception ex) {
            log.warn("Could not navigate from timeline event: {}", ex.getMessage());
        }
    }
}