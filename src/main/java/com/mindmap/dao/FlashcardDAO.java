package com.mindmap.dao;

import com.mindmap.model.Flashcard;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface FlashcardDAO {

    Flashcard insert(Flashcard f) throws SQLException;

    Optional<Flashcard> findById(int id) throws SQLException;

    List<Flashcard> findByUser(int userId) throws SQLException;

    List<Flashcard> findByKnowledgeItem(int knowledgeItemId) throws SQLException;

    /** Case-insensitive search on question + answer. */
    List<Flashcard> searchByUser(int userId, String query) throws SQLException;

    /** Flashcards with a review due today or earlier. */
    List<Flashcard> findDue(int userId) throws SQLException;

    int countByUser(int userId) throws SQLException;

    int countDue(int userId) throws SQLException;

    void update(Flashcard f) throws SQLException;

    void delete(int id) throws SQLException;
}