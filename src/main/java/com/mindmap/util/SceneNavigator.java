package com.mindmap.util;

import com.mindmap.view.knowledge.KnowledgeView;
import com.mindmap.view.source.SourcesView;
import com.mindmap.view.goals.GoalsView;
import com.mindmap.view.views.*;
import com.mindmap.view.graph.GraphView;
import com.mindmap.view.settings.SettingsView;
import com.mindmap.view.flashcard.FlashcardsView;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mindmap.view.review.ReviewView;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.mindmap.view.quiz.QuizView;
import com.mindmap.view.timeline.TimelineView;
import com.mindmap.view.analytics.AnalyticsView;
import com.mindmap.view.dashboard.DashboardView;
/**
 * Central view router. MainLayout owns the sidebar + top bar;
 * only the center content area changes when the user navigates.
 */
public final class SceneNavigator {

    private static final Logger log = LoggerFactory.getLogger(SceneNavigator.class);

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
    /** Pseudo-id — the search results page is not a sidebar item. */
    public static final String SEARCH      = "search";

    private static final SceneNavigator INSTANCE = new SceneNavigator();

    private final Map<String, Supplier<Parent>> views   = new HashMap<>();
    private final Deque<String>                 history = new ArrayDeque<>();

    private String       currentViewId;
    private ContentHost  host;

    private SceneNavigator() { }

    public static SceneNavigator getInstance() { return INSTANCE; }

    public void setHost(ContentHost host) { this.host = host; }

    public void register(String viewId, Supplier<Parent> viewFactory) {
        views.put(viewId, viewFactory);
    }

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

    /** Navigate to a registered view by id. No-op if already active. */
    public void navigateTo(String viewId) {
        navigateTo(viewId, null);
    }

    /**
     * Navigate to a registered view with an optional title override.
     * A null title means use the default pretty title for that view.
     */
    public void navigateTo(String viewId, String titleOverride) {
        if (host == null) {
            log.warn("SceneNavigator host not set.");
            return;
        }
        if (viewId == null) return;
        if (viewId.equals(currentViewId) && titleOverride == null) return;

        Supplier<Parent> factory = views.get(viewId);
        if (factory == null) {
            log.warn("No view registered for id '{}'", viewId);
            return;
        }

        if (currentViewId != null && !viewId.equals(currentViewId)) {
            history.push(currentViewId);
            if (history.size() > 50) history.removeLast();
        }
        currentViewId = viewId;
        host.setContent(factory.get(), viewId, titleOverride);
    }

    /**
     * Shows a one-off, non-registered view (like search results).
     * Sets the current view id so back-navigation still works.
     */
    public void showOneOffView(String viewId, Parent content, String title) {
        if (host == null) return;
        if (currentViewId != null && !viewId.equals(currentViewId)) {
            history.push(currentViewId);
            if (history.size() > 50) history.removeLast();
        }
        currentViewId = viewId;
        host.setContent(content, viewId, title);
    }

    /** Navigate back to the previous view, if any. */
    public void goBack() {
        if (history.isEmpty()) return;
        String previous = history.pop();
        String saved = currentViewId;
        currentViewId = null;
        navigateTo(previous);
        if (!history.isEmpty() && saved != null && history.peek().equals(saved)) {
            history.pop();
        }
    }

    public String getCurrentViewId() { return currentViewId; }

    /** Implemented by MainLayout. Title override is optional (may be null). */
    public interface ContentHost {
        void setContent(Parent content, String viewId, String titleOverride);
    }
}
