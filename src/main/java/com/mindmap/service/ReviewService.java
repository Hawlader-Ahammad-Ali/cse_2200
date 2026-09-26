package com.mindmap.service;

import com.mindmap.algorithm.SM2Scheduler;
import com.mindmap.dao.KnowledgeDAO;
import com.mindmap.dao.ReviewDAO;
import com.mindmap.dao.StudySessionDAO;
import com.mindmap.dao.impl.KnowledgeDAOImpl;
import com.mindmap.dao.impl.ReviewDAOImpl;
import com.mindmap.dao.impl.StudySessionDAOImpl;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Review;
import com.mindmap.model.ReviewLog;
import com.mindmap.model.StudySession;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Spaced repetition orchestration: enroll items, build the queue,
 * apply SM-2 results, track streaks.
 */
public class ReviewService {

    private final ReviewDAO       reviewDAO;
    private final StudySessionDAO sessionDAO;
    private final KnowledgeDAO    knowledgeDAO;

    public ReviewService() {
        this.reviewDAO    = new ReviewDAOImpl();
        this.sessionDAO   = new StudySessionDAOImpl();
        this.knowledgeDAO = new KnowledgeDAOImpl();
    }

    /** Test constructor. */
    public ReviewService(ReviewDAO reviewDAO,
                         StudySessionDAO sessionDAO,
                         KnowledgeDAO knowledgeDAO) {
        this.reviewDAO = reviewDAO;
        this.sessionDAO = sessionDAO;
        this.knowledgeDAO = knowledgeDAO;
    }

    // ------------------------------------------------------------- enrollment

    /**
     * Creates a review row for a newly created knowledge item.
     * Idempotent: does nothing if a row already exists.
     */
    public void enrollKnowledgeItem(int knowledgeItemId) throws ServiceException {
        try {
            if (reviewDAO.findByKnowledgeItem(knowledgeItemId).isPresent()) return;

            Review r = new Review();
            r.setUserId(uid());
            r.setKnowledgeItemId(knowledgeItemId);
            r.setEaseFactor(SM2Scheduler.DEFAULT_EF);
            r.setIntervalDays(0);
            r.setRepetitions(0);
            r.setNextReviewDate(LocalDate.now());
            reviewDAO.insert(r);
        } catch (SQLException e) {
            throw new ServiceException("Could not enroll item for review.", e);
        }
    }

    // ------------------------------------------------------------- queue
    /**
     * Creates a review row for a newly created flashcard.
     * Idempotent: does nothing if a row already exists.
     */
    public void enrollFlashcard(int flashcardId) throws ServiceException {
        try {
            // Look for an existing review for this flashcard
            String checkSql = "SELECT id FROM reviews WHERE flashcard_id = ? LIMIT 1";
            try (var ps = com.mindmap.database.DatabaseManager.getInstance()
                    .getConnection().prepareStatement(checkSql)) {
                ps.setInt(1, flashcardId);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) return;
                }
            }

            Review r = new Review();
            r.setUserId(uid());
            r.setFlashcardId(flashcardId);
            r.setEaseFactor(SM2Scheduler.DEFAULT_EF);
            r.setIntervalDays(0);
            r.setRepetitions(0);
            r.setNextReviewDate(java.time.LocalDate.now());
            reviewDAO.insert(r);
        } catch (SQLException e) {
            throw new ServiceException("Could not enroll flashcard for review.", e);
        }
    }
    /** Reviews due today (or earlier), with knowledge items attached. */
    public List<Review> getDueToday() throws ServiceException {
        try {
            List<Review> due = reviewDAO.findDueByUser(uid(), LocalDate.now());
            return attachItems(due);
        } catch (SQLException e) {
            throw new ServiceException("Could not load review queue.", e);
        }
    }

    public int getDueCount() throws ServiceException {
        try { return reviewDAO.countDueByUser(uid(), LocalDate.now()); }
        catch (SQLException e) { throw new ServiceException("Could not count due items.", e); }
    }

    public int getTotalEnrolled() throws ServiceException {
        try { return reviewDAO.countAllByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count reviews.", e); }
    }

    public double getAverageEase() throws ServiceException {
        try { return reviewDAO.averageEaseFactor(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not compute average ease.", e); }
    }

    public int getReviewedToday() throws ServiceException {
        try { return reviewDAO.countLogsOnDate(uid(), LocalDate.now()); }
        catch (SQLException e) { throw new ServiceException("Could not count today's reviews.", e); }
    }

    // ------------------------------------------------------------- apply

    /**
     * Applies a quality rating to a review, updates the schedule, and
     * records a log entry. Returns the updated review.
     */
    public Review applyResult(Review review, int quality) throws ServiceException {
        try {
            SM2Scheduler.Schedule sched = SM2Scheduler.calculate(
                    quality,
                    review.getRepetitions(),
                    review.getIntervalDays(),
                    review.getEaseFactor());

            review.setEaseFactor(sched.easeFactor());
            review.setIntervalDays(sched.intervalDays());
            review.setRepetitions(sched.repetitions());
            review.setNextReviewDate(sched.nextReviewDate());
            review.setLastReviewDate(LocalDate.now());
            review.setQualityLast(quality);
            reviewDAO.update(review);

            reviewDAO.insertLog(new ReviewLog(review.getId(), quality));
            return review;
        } catch (SQLException e) {
            throw new ServiceException("Could not save review result.", e);
        }
    }

    // ------------------------------------------------------------- session

    /** Starts a new review session; returns the session (id populated). */
    public StudySession startSession() throws ServiceException {
        try {
            StudySession s = new StudySession(uid(), StudySession.Type.REVIEW);
            return sessionDAO.insert(s);
        } catch (SQLException e) {
            throw new ServiceException("Could not start review session.", e);
        }
    }

    public void finishSession(int sessionId, int itemsReviewed, int correctCount)
            throws ServiceException {
        try { sessionDAO.finish(sessionId, itemsReviewed, correctCount); }
        catch (SQLException e) { throw new ServiceException("Could not finish session.", e); }
    }

    // ------------------------------------------------------------- streak

    /**
     * Current streak in days: how many consecutive days (ending today
     * or yesterday) had a completed session.
     */
    public int getStreak() throws ServiceException {
        try {
            List<LocalDate> dates = sessionDAO.findCompletedDates(uid());
            if (dates.isEmpty()) return 0;

            LocalDate today = LocalDate.now();
            LocalDate cursor;

            // Streak counts if there's a session today OR yesterday
            if (dates.get(0).equals(today)) {
                cursor = today;
            } else if (dates.get(0).equals(today.minusDays(1))) {
                cursor = today.minusDays(1);
            } else {
                return 0;
            }

            int streak = 0;
            for (LocalDate d : dates) {
                if (d.equals(cursor)) {
                    streak++;
                    cursor = cursor.minusDays(1);
                } else if (d.isBefore(cursor)) {
                    break;
                }
            }
            return streak;
        } catch (SQLException e) {
            throw new ServiceException("Could not compute streak.", e);
        }
    }

    public int getTotalCompletedSessions() throws ServiceException {
        try { return sessionDAO.countCompleted(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count sessions.", e); }
    }

    // ------------------------------------------------------------- helpers

    private List<Review> attachItems(List<Review> reviews) throws SQLException {
        List<Review> result = new ArrayList<>();
        for (Review r : reviews) {
            if (r.getKnowledgeItemId() != null) {
                Optional<KnowledgeItem> k = knowledgeDAO.findById(r.getKnowledgeItemId());
                if (k.isEmpty()) continue;   // item was deleted — skip
                r.setKnowledgeItem(k.get());
            }
            result.add(r);
        }
        return result;
    }

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }
    /** Lightweight check: is this flashcard enrolled? */
    public boolean isFlashcardEnrolled(int flashcardId) throws ServiceException {
        try {
            String sql = "SELECT 1 FROM reviews WHERE flashcard_id = ? LIMIT 1";
            try (var ps = com.mindmap.database.DatabaseManager.getInstance()
                    .getConnection().prepareStatement(sql)) {
                ps.setInt(1, flashcardId);
                try (var rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not check enrollment.", e);
        }
    }

    /** Get the review record for a flashcard (if any). */
    public Optional<Review> getReviewForFlashcard(int flashcardId) throws ServiceException {
        try {
            String sql = "SELECT id, user_id, knowledge_item_id, flashcard_id, ease_factor, " +
                    "interval_days, repetitions, next_review_date, last_review_date, " +
                    "quality_last, created_at, updated_at " +
                    "FROM reviews WHERE flashcard_id = ? LIMIT 1";
            try (var ps = com.mindmap.database.DatabaseManager.getInstance()
                    .getConnection().prepareStatement(sql)) {
                ps.setInt(1, flashcardId);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setUserId(rs.getInt("user_id"));
                    int kid = rs.getInt("knowledge_item_id");
                    r.setKnowledgeItemId(rs.wasNull() ? null : kid);
                    r.setFlashcardId(rs.getInt("flashcard_id"));
                    r.setEaseFactor(rs.getDouble("ease_factor"));
                    r.setIntervalDays(rs.getInt("interval_days"));
                    r.setRepetitions(rs.getInt("repetitions"));
                    String next = rs.getString("next_review_date");
                    if (next != null) {
                        try { r.setNextReviewDate(java.time.LocalDate.parse(next.substring(0, 10))); }
                        catch (Exception ignore) { }
                    }
                    return Optional.of(r);
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not load review.", e);
        }
    }
}