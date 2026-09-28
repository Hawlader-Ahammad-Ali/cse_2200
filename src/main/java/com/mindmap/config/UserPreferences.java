package com.mindmap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mindmap.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-installation user preferences persisted to JSON.
 * Non-sensitive values only — API keys go into ai-config.properties.
 *
 * <p>Stored at: {@code <AppData>/MindMap/preferences.json}</p>
 */
public final class UserPreferences {

    private static final Logger log = LoggerFactory.getLogger(UserPreferences.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // ----- Appearance -----
    private String theme = "LIGHT";              // LIGHT | DARK
    private String accentColor = "#1ABC9C";
    private int fontSize = 13;
    private boolean showWelcome = true;

    // ----- Window state -----
    private double windowX = Double.NaN;
    private double windowY = Double.NaN;
    private double windowWidth = 1280;
    private double windowHeight = 800;
    private boolean windowMaximized = false;

    // ------------------------------------------------------------- constructor

    public UserPreferences() { }

    // ------------------------------------------------------------- load / save

    private static Path prefsFile() {
        return AppPaths.getAppDataDir().resolve("preferences.json");
    }

    /** Loads preferences from disk, or returns defaults if the file doesn't exist. */
    public static UserPreferences load() {
        Path file = prefsFile();
        if (!Files.exists(file)) {
            log.info("No preferences file found — using defaults");
            return new UserPreferences();
        }
        try {
            return MAPPER.readValue(file.toFile(), UserPreferences.class);
        } catch (IOException e) {
            log.warn("Could not read preferences — using defaults", e);
            return new UserPreferences();
        }
    }

    /** Persists preferences to disk. Never throws. */
    public void save() {
        try {
            Files.createDirectories(prefsFile().getParent());
            MAPPER.writeValue(prefsFile().toFile(), this);
            log.debug("Preferences saved to {}", prefsFile());
        } catch (IOException e) {
            log.warn("Could not save preferences", e);
        }
    }

    // ------------------------------------------------------------- getters / setters

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public boolean isShowWelcome() { return showWelcome; }
    public void setShowWelcome(boolean showWelcome) { this.showWelcome = showWelcome; }

    public double getWindowX() { return windowX; }
    public void setWindowX(double windowX) { this.windowX = windowX; }

    public double getWindowY() { return windowY; }
    public void setWindowY(double windowY) { this.windowY = windowY; }

    public double getWindowWidth() { return windowWidth; }
    public void setWindowWidth(double windowWidth) { this.windowWidth = windowWidth; }

    public double getWindowHeight() { return windowHeight; }
    public void setWindowHeight(double windowHeight) { this.windowHeight = windowHeight; }

    public boolean isWindowMaximized() { return windowMaximized; }
    public void setWindowMaximized(boolean windowMaximized) { this.windowMaximized = windowMaximized; }

    @Override
    public String toString() {
        return "UserPreferences{theme=" + theme
                + ", window=" + windowWidth + "x" + windowHeight
                + ", maximized=" + windowMaximized + "}";
    }
}