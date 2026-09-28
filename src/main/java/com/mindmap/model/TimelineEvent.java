package com.mindmap.model;

import java.time.LocalDateTime;

/**
 * A single entry in the user's learning timeline. Events are read-only
 * projections — they aggregate data that lives in other tables.
 */
public class TimelineEvent {

    public enum Kind {
        SOURCE_ADDED     ("📚", "Source added"),
        KNOWLEDGE_CREATED("🧠", "Knowledge added"),
        KNOWLEDGE_UPDATED("✏",  "Knowledge updated"),
        TAKEAWAY_WRITTEN ("📝", "Takeaway written"),
        REVIEW_COMPLETED ("🔄", "Review completed"),
        QUIZ_COMPLETED   ("📝", "Quiz completed"),
        FLASHCARD_CREATED("🎴", "Flashcard created");

        private final String icon;
        private final String label;

        Kind(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }

        public String getIcon()  { return icon; }
        public String getLabel() { return label; }
    }

    private Kind kind;
    private LocalDateTime timestamp;
    private String title;
    private String subtitle;
    private Integer sourceId;         // if the event involves a source
    private Integer knowledgeItemId;  // if the event involves a knowledge item
    private Integer flashcardId;
    private Integer quizQuestionId;

    public TimelineEvent() { }

    public TimelineEvent(Kind kind, LocalDateTime timestamp, String title, String subtitle) {
        this.kind = kind;
        this.timestamp = timestamp;
        this.title = title;
        this.subtitle = subtitle;
    }

    // ------------------------------------------------------------- accessors

    public Kind getKind() { return kind; }
    public void setKind(Kind k) { this.kind = k; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }

    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String s) { this.subtitle = s; }

    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer s) { this.sourceId = s; }

    public Integer getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Integer k) { this.knowledgeItemId = k; }

    public Integer getFlashcardId() { return flashcardId; }
    public void setFlashcardId(Integer f) { this.flashcardId = f; }

    public Integer getQuizQuestionId() { return quizQuestionId; }
    public void setQuizQuestionId(Integer q) { this.quizQuestionId = q; }
}