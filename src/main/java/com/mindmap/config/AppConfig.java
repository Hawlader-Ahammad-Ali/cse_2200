package com.mindmap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-level configuration and secrets.
 *
 * <p>All secrets are read from environment variables — nothing is hardcoded.
 * This is the ONLY class that should read env vars for sensitive values.</p>
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    public static final String APP_NAME    = "MindMap";
    public static final String APP_VERSION = "1.0.0";

    // AI provider env var keys
    private static final String ENV_AI_KEY      = "MINDMAP_AI_KEY";
    private static final String ENV_AI_BASE_URL = "MINDMAP_AI_BASE_URL";
    private static final String ENV_AI_MODEL    = "MINDMAP_AI_MODEL";

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL    = "gpt-4o-mini";

    private AppConfig() { /* utility class */ }

    /** Reads an env var, trims it, falls back to a default when missing/blank. */
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null) return defaultValue;
        value = value.trim();
        return value.isEmpty() ? defaultValue : value;
    }

    /** AI API key — empty when AI features are disabled. */
    public static String getAIKey() {
        return getEnv(ENV_AI_KEY, "");
    }

    /** True if AI features can be used (a key is configured). */
    public static boolean isAIEnabled() {
        return !getAIKey().isEmpty();
    }

    public static String getAIBaseUrl() {
        return getEnv(ENV_AI_BASE_URL, DEFAULT_BASE_URL);
    }

    public static String getAIModel() {
        return getEnv(ENV_AI_MODEL, DEFAULT_MODEL);
    }

    /** Logs a one-line summary of config at startup (never logs the key). */
    public static void logStartupSummary() {
        log.info("{} v{} — AI enabled: {}", APP_NAME, APP_VERSION, isAIEnabled());
    }
}