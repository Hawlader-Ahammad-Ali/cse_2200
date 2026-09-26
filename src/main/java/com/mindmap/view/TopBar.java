package com.mindmap.view;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.SearchResults;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.RootNavigator;
import com.mindmap.util.SceneNavigator;
import com.mindmap.util.exceptions.ServiceException;
import com.mindmap.view.search.SearchResultsView;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Top bar: view title, global search, theme toggle, user menu.
 * Search now executes against the SearchService on Enter.
 */
public class TopBar extends HBox {

    private static final Logger log = LoggerFactory.getLogger(TopBar.class);

    private final Label      titleLabel  = new Label("Dashboard");
    private final TextField  searchField = new TextField();
    private final Label      themeToggle = new Label("🌙");
    private final MenuButton userMenu    = new MenuButton("👤");

    public TopBar() {
        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        titleLabel.getStyleClass().add("topbar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField.getStyleClass().add("topbar-search");
        searchField.setPromptText("🔍  Search sources, knowledge, tags…   (Ctrl+K)");
        searchField.setTooltip(new Tooltip("Search all your data — press Enter"));
        searchField.setOnAction(e -> performSearch());

        themeToggle.getStyleClass().add("topbar-icon-button");
        themeToggle.setTooltip(new Tooltip("Toggle light / dark theme  (Ctrl+T)"));
        themeToggle.setOnMouseClicked(e -> {
            AppContext.getInstance().toggleTheme();
            refreshThemeIcon();
        });

        buildUserMenu();

        getChildren().addAll(titleLabel, spacer, searchField, themeToggle, userMenu);

        AppContext.getInstance().currentUserProperty().addListener(
                (obs, oldU, newU) -> refreshUserMenuLabel());
        refreshUserMenuLabel();
    }

    // ------------------------------------------------------------ public API

    public void setTitle(String title) { titleLabel.setText(title); }

    public void focusSearch() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    public void refreshThemeIcon() {
        themeToggle.setText(AppContext.getInstance().isDarkTheme() ? "☀" : "🌙");
    }

    // ------------------------------------------------------------ search

    private void performSearch() {
        String q = searchField.getText();
        if (q == null || q.isBlank()) return;

        try {
            SearchResults results = ServiceRegistry.searchService().search(q);
            SearchResultsView view = new SearchResultsView(results);
            SceneNavigator.getInstance().showOneOffView(
                    SceneNavigator.SEARCH,
                    view,
                    "Search: " + q
            );
        } catch (ServiceException e) {
            Dialogs.error("Search failed", e.getMessage());
        }
    }

    // ------------------------------------------------------------ user menu

    private void buildUserMenu() {
        userMenu.getStyleClass().add("topbar-user-menu");

        MenuItem profileItem  = new MenuItem("👤  Profile");
        MenuItem settingsItem = new MenuItem("⚙   Settings");
        MenuItem logoutItem   = new MenuItem("🚪  Sign Out");

        profileItem.setOnAction(e ->
                AppContext.getInstance().setStatusMessage("Profile — coming in a later phase"));

        settingsItem.setOnAction(e ->
                SceneNavigator.getInstance().navigateTo(SceneNavigator.SETTINGS));

        logoutItem.setOnAction(e -> confirmAndLogout());

        userMenu.getItems().addAll(
                profileItem,
                settingsItem,
                new SeparatorMenuItem(),
                logoutItem
        );
    }

    private void refreshUserMenuLabel() {
        var user = AppContext.getInstance().getCurrentUser();
        if (user == null) {
            userMenu.setText("👤");
        } else {
            String label = user.getDisplayLabel();
            if (label != null && label.length() > 14) {
                label = label.substring(0, 14) + "…";
            }
            userMenu.setText("👤  " + label);
        }
    }

    private void confirmAndLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText("Sign out of MindMap?");
        alert.setContentText("Your saved data will remain on this computer.");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.info("User requested logout");
            ServiceRegistry.authService().logout();
            RootNavigator.showLogin();
        }
    }
}