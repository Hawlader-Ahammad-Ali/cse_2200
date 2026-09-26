package com.mindmap.dao.impl;

import com.mindmap.dao.ReviewDAO;
import com.mindmap.model.Review;
import com.mindmap.model.ReviewLog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewDAOImpl extends BaseDAO implements ReviewDAO {

    private static final String COLS =
            "id, user_id, knowledge_item_id, flashcard_id, ease_factor, interval_days, " +
                    "repetitions, next_review_date, last_review_date, quality_last, " +
                    "created_at, updated_at";

    @Override
    public Review insert(Review r) throws SQLException {
        String sql = "INSERT INTO reviews " +
                "(user_id, knowledge_item_id, flashcard_id, ease_factor, interval_days, " +
                " repetitions, next_review_date, last_review_date, quality_last) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            if (r.getKnowledgeItemId() == null) ps.setNull(2, Types.INTEGER);
            else                                ps.setInt(2, r.getKnowledgeItemId());
            if (r.getFlashcardId() == null) ps.setNull(3, Types.INTEGER);
            else                            ps.setInt(3, r.getFlashcardId());
            ps.setDouble(4, r.getEaseFactor());
            ps.setInt(5, r.getIntervalDays());
            ps.setInt(6, r.getRepetitions());
            ps.setString(7, r.getNextReviewDate().toString());
            if (r.getLastReviewDate() == null) ps.setNull(8, Types.VARCHAR);
            else                               ps.setString(8, r.getLastReviewDate().toString());
            if (r.getQualityLast() == null) ps.setNull(9, Types.INTEGER);
            else                            ps.setInt(9, r.getQualityLast());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
        }
        return findById(r.getId()).orElse(r);
    }

    @Override
    public Optional<Review> findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT " + COLS + " FROM reviews WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Review> findByKnowledgeItem(int knowledgeItemId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT " + COLS + " FROM reviews WHERE knowledge_item_id = ? LIMIT 1")) {
            ps.setInt(1, knowledgeItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Review> findByUser(int userId) throws SQLException {
        return queryList("SELECT " + COLS + " FROM reviews WHERE user_id = ? " +
                "ORDER BY next_review_date ASC", userId);
    }

    @Override
    public List<Review> findDueByUser(int userId, LocalDate date) throws SQLException {
        String sql = "SELECT " + COLS + " FROM reviews " +
                "WHERE user_id = ? AND next_review_date <= ? " +
                "ORDER BY next_review_date ASC";
        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public int countDueByUser(int userId, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND next_review_date <= ?")) {
            ps.setInt(1, userId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int countAllByUser(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM reviews WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public double averageEaseFactor(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT AVG(ease_factor) FROM reviews WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : avg;
                }
                return 0.0;
            }
        }
    }

    @Override
    public void update(Review r) throws SQLException {
        String sql = "UPDATE reviews SET " +
                "ease_factor = ?, interval_days = ?, repetitions = ?, " +
                "next_review_date = ?, last_review_date = ?, quality_last = ?, " +
                "updated_at = datetime('now') " +
                "WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, r.getEaseFactor());
            ps.setInt(2, r.getIntervalDays());
            ps.setInt(3, r.getRepetitions());
            ps.setString(4, r.getNextReviewDate().toString());
            if (r.getLastReviewDate() == null) ps.setNull(5, Types.VARCHAR);
            else                               ps.setString(5, r.getLastReviewDate().toString());
            if (r.getQualityLast() == null) ps.setNull(6, Types.INTEGER);
            else                            ps.setInt(6, r.getQualityLast());
            ps.setInt(7, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM reviews WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------- logs

    @Override
    public ReviewLog insertLog(ReviewLog log) throws SQLException {
        String sql = "INSERT INTO review_logs (review_id, quality) VALUES (?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getReviewId());
            ps.setInt(2, log.getQuality());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) log.setId(keys.getInt(1));
            }
        }
        return log;
    }

    @Override
    public List<ReviewLog> findLogsForReview(int reviewId) throws SQLException {
        String sql = "SELECT id, review_id, quality, reviewed_at " +
                "FROM review_logs WHERE review_id = ? ORDER BY reviewed_at DESC";
        List<ReviewLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReviewLog l = new ReviewLog();
                    l.setId(rs.getInt("id"));
                    l.setReviewId(rs.getInt("review_id"));
                    l.setQuality(rs.getInt("quality"));
                    l.setReviewedAt(parseDateTime(rs.getString("reviewed_at")));
                    list.add(l);
                }
            }
        }
        return list;
    }

    @Override
    public int countLogsOnDate(int userId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM review_logs rl " +
                "JOIN reviews r ON r.id = rl.review_id " +
                "WHERE r.user_id = ? AND DATE(rl.reviewed_at) = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ------------------------------------------------------------- internals

    private List<Review> queryList(String sql, int userId) throws SQLException {
        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        int kid = rs.getInt("knowledge_item_id");
        r.setKnowledgeItemId(rs.wasNull() ? null : kid);
        int fid = rs.getInt("flashcard_id");
        r.setFlashcardId(rs.wasNull() ? null : fid);
        r.setEaseFactor(rs.getDouble("ease_factor"));
        r.setIntervalDays(rs.getInt("interval_days"));
        r.setRepetitions(rs.getInt("repetitions"));

        String next = rs.getString("next_review_date");
        if (next != null) {
            try { r.setNextReviewDate(LocalDate.parse(next.substring(0, 10))); }
            catch (Exception ignore) { }
        }
        String last = rs.getString("last_review_date");
        if (last != null && !last.isBlank()) {
            try { r.setLastReviewDate(LocalDate.parse(last.substring(0, 10))); }
            catch (Exception ignore) { }
        }
        int q = rs.getInt("quality_last");
        r.setQualityLast(rs.wasNull() ? null : q);
        r.setCreatedAt(parseDateTime(rs.getString("created_at")));
        r.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return r;
    }
}