package com.mindmap.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A quiz question. Options are stored as a JSON array for MCQ, empty otherwise.
 * For TRUE_FALSE, correctAnswer is "true" or "false".
 * For SHORT, correctAnswer is the expected text (matched fuzzily).
 */
public class QuizQuestion {

    private int id;
    private int userId;
    private Integer knowledgeItemId;
    private QuizQuestionType questionType = QuizQuestionType.MCQ;
    private String questionText;
    private List<String> options = new ArrayList<>();
    private String correctAnswer;
    private String explanation;
    private Origin origin = Origin.USER_CREATED;
    private LocalDateTime createdAt;

    private transient KnowledgeItem knowledgeItem;

    public QuizQuestion() { }

    // ------------------------------------------------------------- accessors

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Integer id) { this.knowledgeItemId = id; }

    public QuizQuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuizQuestionType t) { this.questionType = t; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String q) { this.questionText = q; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> o) {
        this.options = (o == null) ? new ArrayList<>() : new ArrayList<>(o);
    }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String a) { this.correctAnswer = a; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin o) { this.origin = o; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    public KnowledgeItem getKnowledgeItem() { return knowledgeItem; }
    public void setKnowledgeItem(KnowledgeItem k) { this.knowledgeItem = k; }

    @Override
    public String toString() {
        return "QuizQuestion{id=" + id + ", type=" + questionType + "}";
    }
}