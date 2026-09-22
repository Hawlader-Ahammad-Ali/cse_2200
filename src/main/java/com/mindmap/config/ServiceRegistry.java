package com.mindmap.config;

import com.mindmap.service.AuthService;
import com.mindmap.service.KnowledgeService;
import com.mindmap.service.SourceService;

/**
 * Minimal service container (manual dependency injection).
 */
public final class ServiceRegistry {

    private static AuthService      authService;
    private static SourceService    sourceService;
    private static KnowledgeService knowledgeService;

    private ServiceRegistry() { }

    public static synchronized AuthService authService() {
        if (authService == null) authService = new AuthService();
        return authService;
    }

    public static synchronized SourceService sourceService() {
        if (sourceService == null) sourceService = new SourceService();
        return sourceService;
    }

    public static synchronized KnowledgeService knowledgeService() {
        if (knowledgeService == null) knowledgeService = new KnowledgeService();
        return knowledgeService;
    }

    public static synchronized void reset() {
        // Future: reset stateful services
    }
}