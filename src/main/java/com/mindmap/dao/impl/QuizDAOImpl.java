package com.mindmap.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.dao.QuizDAO;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;
import com.mindmap.model.Origin;
import com.mindmap.model.QuizAttempt;
import com.mindmap.model.QuizQuestion;
import com.mindmap.model.QuizQuestionType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizDAOImpl extends BaseDAO implements QuizDAO {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String QCOLS =
            "id, user_id, knowledge_item_id, question_type, question_text, " +
                    "options_json, correct_answer, explanation, origin, created_at";

    // ============================================================= questions

    @Override
    public QuizQuestion insertQuestion(QuizQuestion q) throws SQLException {
        String sql = "INSERT INTO quiz_questions " +
                "(user_id, knowledge_item_id, question_type, question_text, " +
                " options_json, correct_answer, explanation, origin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, q.getUserId());
            if (q.getKnowledgeItemId() == null) ps.setNull(2, Types.INTEGER);
            else                                ps.setInt(2, q.getKnowledgeItemId());
            ps.setString(3, q.getQuestionType().name());
            ps.setString(4, q.getQuestionText());
            ps.setString(5, optionsToJson(q.getOptions()));
            ps.setString(6, q.getCorrectAnswer());
            ps.setString(7, q.getExplanation());
            ps.setString(8, q.getOrigin().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setId(keys.getInt(1));
            }
        }
        return findQuestionById(q.getId()).orElse(q);
    }

    @Override
    public Optional<QuizQuestion> findQuestionById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT " + QCOLS + " FROM quiz_questions WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapQuestion(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<QuizQuestion> findQuestionsByUser(int userId) throws SQLException {
        return queryQuestions("SELECT " + QCOLS + " FROM quiz_questions " +
                "WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    @Override
    public List<QuizQuestion> findQuestionsByKnowledgeItem(int knowledgeItemId) throws SQLException {
        return queryQuestions("SELECT " + QCOLS + " FROM quiz_questions " +
                "WHERE knowledge_item_id = ? ORDER BY created_at DESC", knowledgeItemId);
    }

    @Override
    public int countQuestionsByUser(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM quiz_questions WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void updateQuestion(QuizQuestion q) throws SQLException {
        String sql = "UPDATE quiz_questions SET " +
                "knowledge_item_id = ?, question_type = ?, question_text = ?, " +
                "options_json = ?, correct_answer = ?, explanation = ?, origin = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (q.getKnowledgeItemId() == null) ps.setNull(1, Types.INTEGER);
            else                                ps.setInt(1, q.getKnowledgeItemId());
            ps.setString(2, q.getQuestionType().name());
            ps.setString(3, q.getQuestionText());
            ps.setString(4, optionsToJson(q.getOptions()));
            ps.setString(5, q.getCorrectAnswer());
            ps.setString(6, q.getExplanation());
            ps.setString(7, q.getOrigin().name());
            ps.setInt(8, q.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteQuestion(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM quiz_questions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ============================================================= attempts

    @Override
    public QuizAttempt insertAttempt(QuizAttempt a) throws SQLException {
        String sql = "INSERT INTO quiz_attempts " +
                "(user_id, quiz_question_id, user_answer, is_correct) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getUserId());
            ps.setInt(2, a.getQuizQuestionId());
            ps.setString(3, a.getUserAnswer());
            ps.setInt(4, a.isCorrect() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setId(keys.getInt(1));
            }
        }
        return a;
    }

    @Override
    public List<QuizAttempt> findAttemptsByUser(int userId, int limit) throws SQLException {
        String sql = "SELECT id, user_id, quiz_question_id, user_answer, is_correct, attempted_at " +
                "FROM quiz_attempts WHERE user_id = ? " +
                "ORDER BY attempted_at DESC LIMIT ?";
        List<QuizAttempt> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        }
        return list;
    }

    @Override
    public List<QuizAttempt> findAttemptsForQuestion(int questionId) throws SQLException {
        String sql = "SELECT id, user_id, quiz_question_id, user_answer, is_correct, attempted_at " +
                "FROM quiz_attempts WHERE quiz_question_id = ? " +
                "ORDER BY attempted_at DESC";
        List<QuizAttempt> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        }
        return list;
    }

    @Override
    public List<Integer> findWeakQuestionIds(int userId, int minAttempts, double maxAccuracy)
            throws SQLException {
        String sql = "SELECT quiz_question_id, " +
                "COUNT(*) AS attempts, " +
                "AVG(is_correct) AS accuracy " +
                "FROM quiz_attempts WHERE user_id = ? " +
                "GROUP BY quiz_question_id " +
                "HAVING attempts >= ? AND accuracy <= ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, minAttempts);
            ps.setDouble(3, maxAccuracy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("quiz_question_id"));
            }
        }
        return ids;
    }

    // ============================================================= helpers

    private List<QuizQuestion> queryQuestions(String sql, int param) throws SQLException {
        List<QuizQuestion> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapQuestion(rs));
            }
        }
        return list;
    }

    private QuizQuestion mapQuestion(ResultSet rs) throws SQLException {
        QuizQuestion q = new QuizQuestion();
        q.setId(rs.getInt("id"));
        q.setUserId(rs.getInt("user_id"));
        int kid = rs.getInt("knowledge_item_id");
        q.setKnowledgeItemId(rs.wasNull() ? null : kid);
        q.setQuestionType(QuizQuestionType.fromName(rs.getString("question_type")));
        q.setQuestionText(rs.getString("question_text"));
        q.setOptions(optionsFromJson(rs.getString("options_json")));
        q.setCorrectAnswer(rs.getString("correct_answer"));
        q.setExplanation(rs.getString("explanation"));
        q.setOrigin(Origin.fromName(rs.getString("origin")));
        q.setCreatedAt(parseDateTime(rs.getString("created_at")));
        return q;
    }

    private QuizAttempt mapAttempt(ResultSet rs) throws SQLException {
        QuizAttempt a = new QuizAttempt();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setQuizQuestionId(rs.getInt("quiz_question_id"));
        a.setUserAnswer(rs.getString("user_answer"));
        a.setCorrect(rs.getInt("is_correct") == 1);
        a.setAttemptedAt(parseDateTime(rs.getString("attempted_at")));
        return a;
    }

    private String optionsToJson(List<String> options) {
        if (options == null || options.isEmpty()) return "[]";
        try { return MAPPER.writeValueAsString(options); }
        catch (Exception e) { return "[]"; }
    }

    private List<String> optionsFromJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return MAPPER.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    /** Unused helper kept for future use. */
    @SuppressWarnings("unused")
    private KnowledgeItem loadItem(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT id, title, item_type FROM knowledge_items WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                KnowledgeItem k = new KnowledgeItem();
                k.setId(rs.getInt("id"));
                k.setTitle(rs.getString("title"));
                k.setItemType(KnowledgeType.fromName(rs.getString("item_type")));
                return k;
            }
        }
    }
}