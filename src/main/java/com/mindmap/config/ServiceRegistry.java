package com.mindmap.config;

import com.mindmap.service.AuthService;

/**
 * Minimal service container (manual dependency injection).
 * Future phases will add: sourceService(), knowledgeService(), etc.
 *
 * Thread-safe lazy singletons. Never returns null.
 */
public final class ServiceRegistry {

    private static AuthService authService;

    private ServiceRegistry() { }

    public static synchronized AuthService authService() {
        if (authService == null) {
            authService = new AuthService();
        }
        return authService;
    }

    /** Called on logout to reset stateful services if needed. */
    public static synchronized void reset() {
        // Currently no stateful services to reset — placeholder for future.
    }
}