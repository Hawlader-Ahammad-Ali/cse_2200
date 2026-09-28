package com.mindmap.util;

import com.mindmap.config.UserPreferences;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remembers and restores the main window's position, size, and maximized state.
 *
 * <p>Usage:</p>
 * <pre>
 *   // On app start
 *   UserPreferences prefs = UserPreferences.load();
 *   WindowStateManager.setPreferences(prefs);
 *   ...
 *   WindowStateManager.apply(stage);   // after stage.show()
 *
 *   // On app shutdown
 *   WindowStateManager.save(stage);
 * </pre>
 */
public final class WindowStateManager {

    private static final Logger log = LoggerFactory.getLogger(WindowStateManager.class);

    private static UserPreferences prefs;

    private WindowStateManager() { }

    /** Sets the preferences instance to read/write window state from. */
    public static void setPreferences(UserPreferences p) {
        prefs = p;
    }

    /**
     * Applies saved position/size/maximized state to the given stage.
     * Safe to call on a fresh install (uses defaults).
     */
    public static void apply(Stage stage) {
        if (prefs == null || stage == null) return;
        try {
            // Restore size first (some OSes need this before position)
            if (prefs.getWindowWidth() > 200) {
                stage.setWidth(prefs.getWindowWidth());
            }
            if (prefs.getWindowHeight() > 200) {
                stage.setHeight(prefs.getWindowHeight());
            }

            // Restore position if it was saved
            if (!Double.isNaN(prefs.getWindowX()) && !Double.isNaN(prefs.getWindowY())) {
                stage.setX(prefs.getWindowX());
                stage.setY(prefs.getWindowY());
            }

            // Restore maximized state
            if (prefs.isWindowMaximized()) {
                stage.setMaximized(true);
            }

            log.debug("Window state restored: {}x{} at ({}, {})",
                    prefs.getWindowWidth(), prefs.getWindowHeight(),
                    prefs.getWindowX(), prefs.getWindowY());
        } catch (Exception e) {
            log.warn("Could not restore window state", e);
        }
    }

    /**
     * Saves the current stage state to preferences.
     * Call from {@code Application.stop()} or on window close.
     *
     * <p>If the window is maximized, only the maximized flag is updated —
     * the previous x/y/width/height are preserved so un-maximizing restores
     * the correct size.</p>
     */
    public static void save(Stage stage) {
        if (prefs == null || stage == null) return;
        try {
            if (!stage.isMaximized() && !stage.isFullScreen()) {
                prefs.setWindowX(stage.getX());
                prefs.setWindowY(stage.getY());
                prefs.setWindowWidth(stage.getWidth());
                prefs.setWindowHeight(stage.getHeight());
            }
            prefs.setWindowMaximized(stage.isMaximized());
            prefs.save();
            log.debug("Window state saved");
        } catch (Exception e) {
            log.warn("Could not save window state", e);
        }
    }
}