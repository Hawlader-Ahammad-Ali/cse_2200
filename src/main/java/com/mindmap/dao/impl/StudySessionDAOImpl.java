package com.mindmap.dao.impl;

import com.mindmap.dao.StudySessionDAO;
import com.mindmap.model.StudySession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudySessionDAOImpl extends BaseDAO implements StudySessionDAO {

    @Override
    public StudySession insert(StudySession s) throws SQLException {
        String sql = "INSERT INTO study_sessions " +
                "(user_id, session_type, start_time) VALUES (?, ?, datetime('now'))";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getSessionType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
        }
        return s;
    }

    @Override
    public void finish(int sessionId, int itemsReviewed, int correctCount) throws SQLException {
        String sql = "UPDATE study_sessions SET " +
                "end_time = datetime('now'), items_reviewed = ?, correct_count = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemsReviewed);
            ps.setInt(2, correctCount);
            ps.setInt(3, sessionId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<LocalDate> findCompletedDates(int userId) throws SQLException {
        String sql = "SELECT DISTINCT DATE(end_time) AS d FROM study_sessions " +
                "WHERE user_id = ? AND end_time IS NOT NULL " +
                "ORDER BY d DESC";
        List<LocalDate> dates = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String d = rs.getString("d");
                    if (d != null) {
                        try { dates.add(LocalDate.parse(d)); } catch (Exception ignore) { }
                    }
                }
            }
        }
        return dates;
    }

    @Override
    public List<StudySession> findCompletedOn(int userId, LocalDate date) throws SQLException {
        String sql = "SELECT id, user_id, session_type, start_time, end_time, " +
                "items_reviewed, correct_count, notes " +
                "FROM study_sessions WHERE user_id = ? AND DATE(end_time) = ?";
        List<StudySession> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudySession s = new StudySession();
                    s.setId(rs.getInt("id"));
                    s.setUserId(rs.getInt("user_id"));
                    try { s.setSessionType(StudySession.Type.valueOf(rs.getString("session_type"))); }
                    catch (Exception ignore) { }
                    s.setStartTime(parseDateTime(rs.getString("start_time")));
                    s.setEndTime(parseDateTime(rs.getString("end_time")));
                    s.setItemsReviewed(rs.getInt("items_reviewed"));
                    s.setCorrectCount(rs.getInt("correct_count"));
                    s.setNotes(rs.getString("notes"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    @Override
    public int countCompleted(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM study_sessions WHERE user_id = ? AND end_time IS NOT NULL")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}