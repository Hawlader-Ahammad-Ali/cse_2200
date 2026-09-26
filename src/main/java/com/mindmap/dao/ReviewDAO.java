package com.mindmap.dao;

import com.mindmap.model.Review;
import com.mindmap.model.ReviewLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReviewDAO {

    Review insert(Review r) throws SQLException;

    Optional<Review> findById(int id) throws SQLException;

    Optional<Review> findByKnowledgeItem(int knowledgeItemId) throws SQLException;

    /** All reviews for a user, ordered by next_review_date ascending. */
    List<Review> findByUser(int userId) throws SQLException;

    /** Reviews due on or before the given date. */
    List<Review> findDueByUser(int userId, LocalDate date) throws SQLException;

    int countDueByUser(int userId, LocalDate date) throws SQLException;

    int countAllByUser(int userId) throws SQLException;

    /** Average ease factor across the user's reviews (0 if none). */
    double averageEaseFactor(int userId) throws SQLException;

    void update(Review r) throws SQLException;

    void delete(int id) throws SQLException;

    // ------------------------------------------------------------- logs

    ReviewLog insertLog(ReviewLog log) throws SQLException;

    List<ReviewLog> findLogsForReview(int reviewId) throws SQLException;

    /** Count of log entries for a user on a given day. */
    int countLogsOnDate(int userId, LocalDate date) throws SQLException;
}