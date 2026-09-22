package com.mindmap.view;

import com.mindmap.util.SceneNavigator;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Left navigation sidebar. Each item is a styled Label that routes through
 * {@link SceneNavigator}. The currently active item receives
 * {@code sidebar-item-active} for highlighted styling.
 */
public class Sidebar extends VBox {

    private static final String ITEM_STYLE   = "sidebar-item";
    private static final String ACTIVE_STYLE = "sidebar-item-active";

    private final Map<String, Label> itemLabels = new LinkedHashMap<>();
    private String activeViewId;

    public Sidebar() {
        getStyleClass().add("sidebar");
        setAlignment(Pos.TOP_LEFT);

        Label logo = new Label("🧠  MindMap");
        logo.getStyleClass().add("sidebar-logo");
        getChildren().add(logo);

        addItem("🏠", "Dashboard",       SceneNavigator.DASHBOARD);
        addItem("📚", "Sources",         SceneNavigator.SOURCES);
        addItem("🧠", "Knowledge",       SceneNavigator.KNOWLEDGE);
        addItem("🕸",  "Knowledge Graph", SceneNavigator.GRAPH);

        addSeparator();

        addItem("🔄", "Review",          SceneNavigator.REVIEW);
        addItem("🎴", "Flashcards",      SceneNavigator.FLASHCARDS);
        addItem("📝", "Quiz",            SceneNavigator.QUIZ);

        addSeparator();

        addItem("📅", "Timeline",        SceneNavigator.TIMELINE);
        addItem("📊", "Analytics",       SceneNavigator.ANALYTICS);
        addItem("🎯", "Goals",           SceneNavigator.GOALS);

        addSeparator();

        addItem("⚙",  "Settings",        SceneNavigator.SETTINGS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("sidebar-logo");
        version.setStyle("-fx-font-size: 10.5px; -fx-opacity: 0.5;");
        getChildren().add(version);
    }

    /** Highlight the item that corresponds to the current view. */
    public void setActive(String viewId) {
        if (viewId == null) return;
        if (activeViewId != null) {
            Label prev = itemLabels.get(activeViewId);
            if (prev != null) prev.getStyleClass().remove(ACTIVE_STYLE);
        }
        Label curr = itemLabels.get(viewId);
        if (curr != null && !curr.getStyleClass().contains(ACTIVE_STYLE)) {
            curr.getStyleClass().add(ACTIVE_STYLE);
        }
        activeViewId = viewId;
    }

    // ---------------------------------------------------------------- internals

    private void addItem(String icon, String text, String viewId) {
        Label item = new Label("  " + icon + "   " + text);
        item.getStyleClass().add(ITEM_STYLE);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setOnMouseClicked(e -> SceneNavigator.getInstance().navigateTo(viewId));
        getChildren().add(item);
        itemLabels.put(viewId, item);
    }

    private void addSeparator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("sidebar-separator");
        getChildren().add(sep);
    }
}