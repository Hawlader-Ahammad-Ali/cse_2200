package com.mindmap.dao;

import com.mindmap.model.QuizAttempt;
import com.mindmap.model.QuizQuestion;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface QuizDAO {

    QuizQuestion insertQuestion(QuizQuestion q) throws SQLException;

    Optional<QuizQuestion> findQuestionById(int id) throws SQLException;

    List<QuizQuestion> findQuestionsByUser(int userId) throws SQLException;

    List<QuizQuestion> findQuestionsByKnowledgeItem(int knowledgeItemId) throws SQLException;

    int countQuestionsByUser(int userId) throws SQLException;

    void updateQuestion(QuizQuestion q) throws SQLException;

    void deleteQuestion(int id) throws SQLException;

    // ------------------------------------------------------------- attempts

    QuizAttempt insertAttempt(QuizAttempt a) throws SQLException;

    List<QuizAttempt> findAttemptsByUser(int userId, int limit) throws SQLException;

    List<QuizAttempt> findAttemptsForQuestion(int questionId) throws SQLException;

    /** Returns question ids whose most recent N attempts are predominantly wrong. */
    List<Integer> findWeakQuestionIds(int userId, int minAttempts, double maxAccuracy) throws SQLException;
}