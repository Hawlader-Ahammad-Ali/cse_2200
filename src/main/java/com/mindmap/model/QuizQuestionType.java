package com.mindmap.model;

public enum QuizQuestionType {
    MCQ       ("Multiple choice"),
    TRUE_FALSE("True / False"),
    SHORT     ("Short answer");

    private final String label;

    QuizQuestionType(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static QuizQuestionType fromName(String name) {
        if (name == null) return MCQ;
        try { return QuizQuestionType.valueOf(name); }
        catch (IllegalArgumentException e) { return MCQ; }
    }

    @Override public String toString() { return label; }
}