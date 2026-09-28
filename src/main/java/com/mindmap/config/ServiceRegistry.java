package com.mindmap.config;

import com.mindmap.service.*;

/**
 * Minimal service container (manual dependency injection).
 */
public final class ServiceRegistry {

    private static AuthService      authService;
    private static SourceService    sourceService;
    private static KnowledgeService knowledgeService;
    private static SearchService    searchService;
    private static GraphService     graphService;
    private static AIService        aiService;
    private static ReviewService    reviewService;
    private static FlashcardService flashcardService;
    private static QuizService      quizService;
    private static TimelineService  timelineService;
    private static AnalyticsService analyticsService;
    private static BackupService    backupService;
    private static GoalService      goalService;

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
    public static synchronized SearchService searchService() {
        if (searchService == null) searchService = new SearchService();
        return searchService;
    }
    public static synchronized GraphService graphService() {
        if (graphService == null) graphService = new GraphService();
        return graphService;
    }
    public static synchronized AIService aiService() {
        if (aiService == null) aiService = new AIService();
        return aiService;
    }
    public static synchronized ReviewService reviewService() {
        if (reviewService == null) reviewService = new ReviewService();
        return reviewService;
    }
    public static synchronized FlashcardService flashcardService() {
        if (flashcardService == null) flashcardService = new FlashcardService();
        return flashcardService;
    }
    public static synchronized QuizService quizService() {
        if (quizService == null) quizService = new QuizService();
        return quizService;
    }
    public static synchronized TimelineService timelineService() {
        if (timelineService == null) timelineService = new TimelineService();
        return timelineService;
    }
    public static synchronized AnalyticsService analyticsService() {
        if (analyticsService == null) analyticsService = new AnalyticsService();
        return analyticsService;
    }
    public static synchronized BackupService backupService() {
        if (backupService == null) backupService = new BackupService();
        return backupService;
    }

    public static synchronized GoalService goalService() {
        if (goalService == null) goalService = new GoalService();
        return goalService;
    }

    public static synchronized void reset() { }
}