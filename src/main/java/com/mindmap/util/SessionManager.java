package com.mindmap.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Persists a lightweight session (user id + username) between app launches,
 * enabling auto-login. Stored in {@code session.properties} inside the app
 * data directory — NOT a security token, just a convenience pointer.
 */
public final class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private static final String FILE_NAME    = "session.properties";
    private static final String KEY_USER_ID  = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SAVED_AT = "savedAt";

    private SessionManager() { }

    private static Path sessionFile() {
        return AppPaths.getAppDataDir().resolve(FILE_NAME);
    }

    /** Saves the session. Silently fails on IO errors (never blocks login). */
    public static void saveSession(int userId, String username) {
        Properties props = new Properties();
        props.setProperty(KEY_USER_ID,  String.valueOf(userId));
        props.setProperty(KEY_USERNAME, username == null ? "" : username);
        props.setProperty(KEY_SAVED_AT, LocalDateTime.now().toString());

        Path file = sessionFile();
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "MindMap session — do not edit manually");
            log.debug("Session saved for user {}", userId);
        } catch (IOException e) {
            log.warn("Could not save session file", e);
        }
    }

    /** @return the saved user ID, or {@code null} if no session exists or file is unreadable. */
    public static Integer loadUserId() {
        Path file = sessionFile();
        if (!Files.exists(file)) return null;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            String raw = props.getProperty(KEY_USER_ID);
            if (raw == null || raw.isBlank()) return null;
            return Integer.parseInt(raw);
        } catch (IOException | NumberFormatException e) {
            log.warn("Could not read session file — ignoring", e);
            return null;
        }
    }

    /** Deletes the session file. Safe to call when no session exists. */
    public static void clearSession() {
        try {
            Files.deleteIfExists(sessionFile());
            log.debug("Session cleared.");
        } catch (IOException e) {
            log.warn("Could not delete session file", e);
        }
    }

    public static boolean hasSession() {
        return loadUserId() != null;
    }
}