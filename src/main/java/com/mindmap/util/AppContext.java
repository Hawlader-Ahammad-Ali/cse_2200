package com.mindmap.util;

import com.mindmap.model.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Global application session state.
 * Holds the logged-in user, current theme, and status bar message.
 * All values are JavaFX properties so views can bind to them.
 */
public final class AppContext {

    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK  = "DARK";

    private static final AppContext INSTANCE = new AppContext();

    private final ObjectProperty<User> currentUser   = new SimpleObjectProperty<>(null);
    private final ObjectProperty<String> theme       = new SimpleObjectProperty<>(THEME_LIGHT);
    private final ObjectProperty<String> statusMsg   = new SimpleObjectProperty<>("Ready");

    private AppContext() { }

    public static AppContext getInstance() { return INSTANCE; }

    // ---------------------------------------------------------------- user
    public ObjectProperty<User> currentUserProperty() { return currentUser; }
    public User getCurrentUser() { return currentUser.get(); }
    public void setCurrentUser(User user) { currentUser.set(user); }
    public boolean isLoggedIn() { return currentUser.get() != null; }
    public void logout() {
        currentUser.set(null);
        statusMsg.set("Logged out");
    }

    // ---------------------------------------------------------------- theme
    public ObjectProperty<String> themeProperty() { return theme; }
    public String getTheme() { return theme.get(); }
    public void setTheme(String name) {
        theme.set(THEME_DARK.equals(name) ? THEME_DARK : THEME_LIGHT);
    }
    public boolean isDarkTheme() { return THEME_DARK.equals(theme.get()); }
    public void toggleTheme() { setTheme(isDarkTheme() ? THEME_LIGHT : THEME_DARK); }

    // ---------------------------------------------------------------- status
    public ObjectProperty<String> statusMessageProperty() { return statusMsg; }
    public String getStatusMessage() { return statusMsg.get(); }
    public void setStatusMessage(String msg) { statusMsg.set(msg == null ? "" : msg); }
}