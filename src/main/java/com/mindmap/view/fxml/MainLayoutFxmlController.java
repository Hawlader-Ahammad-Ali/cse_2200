package com.mindmap.view.fxml;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.util.AppContext;
import com.mindmap.util.RootNavigator;
import com.mindmap.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * FXML controller for main_layout.fxml.
 * Routes to programmatic views from Phases 5–15 (Sources, Knowledge, Graph, etc.)
 * because those views are Java-built. This FXML shell is a working alternative
 * that could be extended to FXML one view at a time.
 */
public class MainLayoutFxmlController {

    private static final Logger log = LoggerFactory.getLogger(MainLayoutFxmlController.class);

    @FXML private VBox sidebar;
    @FXML private Label viewTitle;
    @FXML private TextField searchField;
    @FXML private Label themeToggle;
    @FXML private MenuButton userMenu;
    @FXML private StackPane contentArea;
    @FXML private Label statusLabel;

    private Label activeNavItem;

    @FXML
    private void initialize() {
        // Listen to global status
        AppContext.getInstance().statusMessageProperty().addListener(
                (o, ov, nv) -> statusLabel.setText(nv == null ? "" : nv));

        // Show current user
        if (AppContext.getInstance().isLoggedIn()) {
            userMenu.setText("👤  " + AppContext.getInstance().getCurrentUser().getDisplayLabel());
        }

        // Default view
        navigateDashboard(null);
    }

    // ------------------------------------------------------------- navigation

    @FXML private void navigateDashboard(MouseEvent e)  { loadProgrammaticView(SceneNavigator.DASHBOARD, "Dashboard", (Label) e.getSource()); }
    @FXML private void navigateSources(MouseEvent e)    { loadProgrammaticView(SceneNavigator.SOURCES,   "Sources",   (Label) e.getSource()); }
    @FXML private void navigateKnowledge(MouseEvent e)  { loadProgrammaticView(SceneNavigator.KNOWLEDGE, "Knowledge", (Label) e.getSource()); }
    @FXML private void navigateGraph(MouseEvent e)      { loadProgrammaticView(SceneNavigator.GRAPH,     "Knowledge Graph", (Label) e.getSource()); }
    @FXML private void navigateReview(MouseEvent e)     { loadProgrammaticView(SceneNavigator.REVIEW,    "Review",    (Label) e.getSource()); }
    @FXML private void navigateFlashcards(MouseEvent e) { loadProgrammaticView(SceneNavigator.FLASHCARDS,"Flashcards",(Label) e.getSource()); }
    @FXML private void navigateQuiz(MouseEvent e)       { loadProgrammaticView(SceneNavigator.QUIZ,      "Quiz",      (Label) e.getSource()); }
    @FXML private void navigateTimeline(MouseEvent e)   { loadProgrammaticView(SceneNavigator.TIMELINE,  "Timeline",  (Label) e.getSource()); }
    @FXML private void navigateAnalytics(MouseEvent e)  { loadProgrammaticView(SceneNavigator.ANALYTICS, "Analytics", (Label) e.getSource()); }
    @FXML private void navigateGoals(MouseEvent e)      { loadProgrammaticView(SceneNavigator.GOALS,     "Goals",     (Label) e.getSource()); }
    @FXML private void navigateSettings(MouseEvent e)   { loadProgrammaticView(SceneNavigator.SETTINGS,  "Settings",  (Label) e.getSource()); }

    private void loadProgrammaticView(String viewId, String title, Label source) {
        try {
            // Delegate to the same SceneNavigator that the programmatic UI uses
            SceneNavigator.getInstance().navigateTo(viewId);
            viewTitle.setText(title);

            // Highlight the active sidebar item
            if (activeNavItem != null) {
                activeNavItem.getStyleClass().remove("sidebar-item-active");
            }
            if (source != null) {
                source.getStyleClass().add("sidebar-item-active");
                activeNavItem = source;
            }
        } catch (Exception ex) {
            log.error("Could not navigate to {}", viewId, ex);
        }
    }

    // ------------------------------------------------------------- actions

    @FXML
    private void handleSearch(ActionEvent e) {
        String q = searchField.getText();
        if (q != null && !q.isBlank()) {
            AppContext.getInstance().setStatusMessage("Search (Phase 7 wired): " + q);
        }
    }

    @FXML
    private void handleToggleTheme(MouseEvent e) {
        AppContext.getInstance().toggleTheme();
        themeToggle.setText(AppContext.getInstance().isDarkTheme() ? "☀" : "🌙");
    }

    @FXML
    private void handleLogout() {
        if (com.mindmap.util.Dialogs.confirm("Sign out?", "Your data remains on this computer.")) {
            ServiceRegistry.authService().logout();
            RootNavigator.showLogin();
        }
    }
}