package com.mindmap.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A spaced repetition record for either a knowledge item or a flashcard.
 * Exactly one of {@code knowledgeItemId} / {@code flashcardId} is set
 * (enforced by the DB CHECK constraint).
 */
public class Review {

    private int id;
    private int userId;
    private Integer knowledgeItemId;
    private Integer flashcardId;
    private double easeFactor = 2.5;
    private int intervalDays;
    private int repetitions;
    private LocalDate nextReviewDate;
    private LocalDate lastReviewDate;
    private Integer qualityLast;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Populated when loaded for a review session — not a DB column
    private transient KnowledgeItem knowledgeItem;

    public Review() { }

    // ------------------------------------------------------------- accessors

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Integer id) { this.knowledgeItemId = id; }

    public Integer getFlashcardId() { return flashcardId; }
    public void setFlashcardId(Integer id) { this.flashcardId = id; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double ef) { this.easeFactor = ef; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int d) { this.intervalDays = d; }

    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int r) { this.repetitions = r; }

    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate d) { this.nextReviewDate = d; }

    public LocalDate getLastReviewDate() { return lastReviewDate; }
    public void setLastReviewDate(LocalDate d) { this.lastReviewDate = d; }

    public Integer getQualityLast() { return qualityLast; }
    public void setQualityLast(Integer q) { this.qualityLast = q; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }

    public KnowledgeItem getKnowledgeItem() { return knowledgeItem; }
    public void setKnowledgeItem(KnowledgeItem k) { this.knowledgeItem = k; }

    /** True if this review is due today or earlier. */
    public boolean isDue() {
        return nextReviewDate != null && !nextReviewDate.isAfter(LocalDate.now());
    }
}