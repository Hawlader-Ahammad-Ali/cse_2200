package com.mindmap.model;

import java.time.LocalDateTime;

/**
 * A manually-created or AI-generated flashcard.
 * Optionally linked to a knowledge item it was derived from.
 */
public class Flashcard {

    private int id;
    private int userId;
    private Integer knowledgeItemId;    // nullable
    private String question;
    private String answer;
    private Origin origin = Origin.USER_CREATED;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient — populated on demand by the DAO
    private transient KnowledgeItem knowledgeItem;

    public Flashcard() { }

    public Flashcard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Integer id) { this.knowledgeItemId = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }

    public String getAnswer() { return answer; }
    public void setAnswer(String a) { this.answer = a; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin o) { this.origin = o; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }

    public KnowledgeItem getKnowledgeItem() { return knowledgeItem; }
    public void setKnowledgeItem(KnowledgeItem k) { this.knowledgeItem = k; }

    @Override
    public String toString() {
        return "Flashcard{id=" + id + ", q='" + question + "'}";
    }
}