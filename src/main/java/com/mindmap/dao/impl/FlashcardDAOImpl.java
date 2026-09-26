package com.mindmap.dao.impl;

import com.mindmap.dao.FlashcardDAO;
import com.mindmap.model.Flashcard;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;
import com.mindmap.model.Origin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlashcardDAOImpl extends BaseDAO implements FlashcardDAO {

    private static final String COLS =
            "id, user_id, knowledge_item_id, question, answer, origin, created_at, updated_at";

    @Override
    public Flashcard insert(Flashcard f) throws SQLException {
        String sql = "INSERT INTO flashcards " +
                "(user_id, knowledge_item_id, question, answer, origin) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getUserId());
            if (f.getKnowledgeItemId() == null) ps.setNull(2, Types.INTEGER);
            else                                ps.setInt(2, f.getKnowledgeItemId());
            ps.setString(3, f.getQuestion());
            ps.setString(4, f.getAnswer());
            ps.setString(5, f.getOrigin().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) f.setId(keys.getInt(1));
            }
        }
        return findById(f.getId()).orElse(f);
    }

    @Override
    public Optional<Flashcard> findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT " + COLS + " FROM flashcards WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Flashcard> findByUser(int userId) throws SQLException {
        return queryList("SELECT " + COLS + " FROM flashcards " +
                "WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    @Override
    public List<Flashcard> findByKnowledgeItem(int knowledgeItemId) throws SQLException {
        List<Flashcard> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT " + COLS + " FROM flashcards WHERE knowledge_item_id = ? " +
                        "ORDER BY created_at DESC")) {
            ps.setInt(1, knowledgeItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<Flashcard> searchByUser(int userId, String query) throws SQLException {
        String sql = "SELECT " + COLS + " FROM flashcards WHERE user_id = ? AND (" +
                "LOWER(question) LIKE LOWER(?) OR LOWER(answer) LIKE LOWER(?)) " +
                "ORDER BY created_at DESC";
        String pattern = "%" + query.trim() + "%";
        List<Flashcard> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<Flashcard> findDue(int userId) throws SQLException {
        String today = LocalDate.now().toString();
        String sql = "SELECT f.id, f.user_id, f.knowledge_item_id, f.question, f.answer, " +
                "f.origin, f.created_at, f.updated_at " +
                "FROM flashcards f " +
                "JOIN reviews r ON r.flashcard_id = f.id " +
                "WHERE f.user_id = ? AND r.next_review_date <= ? " +
                "ORDER BY r.next_review_date ASC";
        List<Flashcard> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, today);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public int countByUser(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM flashcards WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int countDue(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM flashcards f JOIN reviews r ON r.flashcard_id = f.id " +
                        "WHERE f.user_id = ? AND r.next_review_date <= ?")) {
            ps.setInt(1, userId);
            ps.setString(2, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void update(Flashcard f) throws SQLException {
        String sql = "UPDATE flashcards SET " +
                "knowledge_item_id = ?, question = ?, answer = ?, origin = ?, " +
                "updated_at = datetime('now') WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (f.getKnowledgeItemId() == null) ps.setNull(1, Types.INTEGER);
            else                                ps.setInt(1, f.getKnowledgeItemId());
            ps.setString(2, f.getQuestion());
            ps.setString(3, f.getAnswer());
            ps.setString(4, f.getOrigin().name());
            ps.setInt(5, f.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM flashcards WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------- internals

    private List<Flashcard> queryList(String sql, int param) throws SQLException {
        List<Flashcard> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Flashcard mapRow(ResultSet rs) throws SQLException {
        Flashcard f = new Flashcard();
        f.setId(rs.getInt("id"));
        f.setUserId(rs.getInt("user_id"));
        int kid = rs.getInt("knowledge_item_id");
        f.setKnowledgeItemId(rs.wasNull() ? null : kid);
        f.setQuestion(rs.getString("question"));
        f.setAnswer(rs.getString("answer"));
        f.setOrigin(Origin.fromName(rs.getString("origin")));
        f.setCreatedAt(parseDateTime(rs.getString("created_at")));
        f.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return f;
    }

    /** Small helper for callers that want to skip loading the item. */
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