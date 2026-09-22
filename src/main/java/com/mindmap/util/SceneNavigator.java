package com.mindmap.util;

import com.mindmap.view.views.*;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central view router. The main window has a fixed sidebar + top bar;
 * only the center content area changes when the user navigates.
 */
public final class SceneNavigator {

    private static final Logger log = LoggerFactory.getLogger(SceneNavigator.class);

    // Public view identifiers
    public static final String DASHBOARD   = "dashboard";
    public static final String SOURCES     = "sources";
    public static final String KNOWLEDGE   = "knowledge";
    public static final String GRAPH       = "graph";
    public static final String REVIEW      = "review";
    public static final String FLASHCARDS  = "flashcards";
    public static final String QUIZ        = "quiz";
    public static final String TIMELINE    = "timeline";
    public static final String ANALYTICS   = "analytics";
    public static final String GOALS       = "goals";
    public static final String SETTINGS    = "settings";

    private static final SceneNavigator INSTANCE = new SceneNavigator();

    private final Map<String, Supplier<Parent>> views   = new HashMap<>();
    private final Deque<String>                 history = new ArrayDeque<>();

    private String       currentViewId;
    private ContentHost  host;

    private SceneNavigator() { }

    public static SceneNavigator getInstance() { return INSTANCE; }

    /** MainLayout sets itself as the host so views can be injected. */
    public void setHost(ContentHost host) {
        this.host = host;
    }

    /** Register a view factory (called once at startup). */
    public void register(String viewId, Supplier<Parent> viewFactory) {
        views.put(viewId, viewFactory);
    }

    /** Registers all Phase-2 views. */
    public void registerDefaultViews() {
        register(DASHBOARD,  DashboardView::new);
        register(SOURCES,    SourcesView::new);
        register(KNOWLEDGE,  KnowledgeView::new);
        register(GRAPH,      GraphView::new);
        register(REVIEW,     ReviewView::new);
        register(FLASHCARDS, FlashcardsView::new);
        register(QUIZ,       QuizView::new);
        register(TIMELINE,   TimelineView::new);
        register(ANALYTICS,  AnalyticsView::new);
        register(GOALS,      GoalsView::new);
        register(SETTINGS,   SettingsView::new);
        log.debug("Registered {} views", views.size());
    }

    /** Navigate to a view by id. No-op if already active. */
    public void navigateTo(String viewId) {
        if (host == null) {
            log.warn("SceneNavigator host not set.");
            return;
        }
        if (viewId == null || viewId.equals(currentViewId)) return;

        Supplier<Parent> factory = views.get(viewId);
        if (factory == null) {
            log.warn("No view registered for id '{}'", viewId);
            return;
        }

        if (currentViewId != null) {
            history.push(currentViewId);
            if (history.size() > 50) history.removeLast();
        }
        currentViewId = viewId;
        host.setContent(factory.get(), viewId);
    }

    /** Navigate back to the previous view, if any. */
    public void goBack() {
        if (history.isEmpty()) return;
        String previous = history.pop();
        // Temporarily clear so navigateTo doesn't re-push
        String saved = currentViewId;
        currentViewId = null;
        navigateTo(previous);
        // Avoid duplicated history entry for the view we came from
        if (!history.isEmpty() && saved != null && history.peek().equals(saved)) {
            history.pop();
        }
    }

    public String getCurrentViewId() { return currentViewId; }

    /** Implemented by MainLayout. */
    public interface ContentHost {
        void setContent(Parent content, String viewId);
    }
}