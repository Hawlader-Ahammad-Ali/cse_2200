package com.mindmap.model;

import java.time.LocalDateTime;

/**
 * A study session used for streak tracking and analytics.
 */
public class StudySession {

    public enum Type { REVIEW, QUIZ, EXPLORE }

    private int id;
    private int userId;
    private Type sessionType = Type.REVIEW;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int itemsReviewed;
    private int correctCount;
    private String notes;

    public StudySession() { }

    public StudySession(int userId, Type type) {
        this.userId = userId;
        this.sessionType = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int u) { this.userId = u; }

    public Type getSessionType() { return sessionType; }
    public void setSessionType(Type t) { this.sessionType = t; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime t) { this.startTime = t; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime t) { this.endTime = t; }

    public int getItemsReviewed() { return itemsReviewed; }
    public void setItemsReviewed(int n) { this.itemsReviewed = n; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int n) { this.correctCount = n; }

    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }

    public boolean isComplete() { return endTime != null; }
}