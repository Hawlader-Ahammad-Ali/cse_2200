package com.mindmap.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves OS-specific application directories and ensures they exist.
 *
 * <p>Windows : %APPDATA%\MindMap\
 * <br>macOS  : ~/Library/Application Support/MindMap/
 * <br>Other  : ~/.MindMap/</p>
 */
public final class AppPaths {

    private static final Logger log = LoggerFactory.getLogger(AppPaths.class);

    private static final String APP_DIR_NAME = "MindMap";

    private AppPaths() { /* utility class */ }

    /** Root data directory for the app. */
    public static Path getAppDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, APP_DIR_NAME);
            }
            return Paths.get(System.getProperty("user.home"), "." + APP_DIR_NAME);
        }

        if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support", APP_DIR_NAME);
        }

        // Linux / Unix
        return Paths.get(System.getProperty("user.home"), "." + APP_DIR_NAME);
    }

    /** Full path to the SQLite database file. */
    public static Path getDatabaseFile() {
        return getAppDataDir().resolve("mindmap.db");
    }

    /** Path to the backups directory. */
    public static Path getBackupsDir() {
        return getAppDataDir().resolve("backups");
    }

    /** Path to the attachments directory. */
    public static Path getAttachmentsDir() {
        return getAppDataDir().resolve("attachments");
    }

    /** Path to the exports directory (PDF, etc.). */
    public static Path getExportsDir() {
        return getAppDataDir().resolve("exports");
    }

    /** Path to the log file. */
    public static Path getLogFile() {
        return getAppDataDir().resolve("mindmap.log");
    }

    /**
     * Creates all application directories if they don't exist yet.
     * Safe to call multiple times.
     */
    public static void ensureDirectories() {
        try {
            Files.createDirectories(getAppDataDir());
            Files.createDirectories(getBackupsDir());
            Files.createDirectories(getAttachmentsDir());
            Files.createDirectories(getExportsDir());
            log.debug("App data directory ready: {}", getAppDataDir());
        } catch (IOException e) {
            // Non-fatal at this stage — DB open will fail loudly later if needed.
            log.error("Could not create app data directories", e);
            throw new IllegalStateException(
                    "Failed to create application directories at " + getAppDataDir(), e);
        }
    }
}