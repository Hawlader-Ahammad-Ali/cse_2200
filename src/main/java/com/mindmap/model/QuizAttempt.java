package com.mindmap.model;

import java.time.LocalDateTime;

public class QuizAttempt {

    private int id;
    private int userId;
    private int quizQuestionId;
    private String userAnswer;
    private boolean correct;
    private LocalDateTime attemptedAt;

    public QuizAttempt() { }

    public QuizAttempt(int quizQuestionId, String userAnswer, boolean correct) {
        this.quizQuestionId = quizQuestionId;
        this.userAnswer = userAnswer;
        this.correct = correct;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int u) { this.userId = u; }

    public int getQuizQuestionId() { return quizQuestionId; }
    public void setQuizQuestionId(int q) { this.quizQuestionId = q; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String a) { this.userAnswer = a; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean c) { this.correct = c; }

    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime t) { this.attemptedAt = t; }
}