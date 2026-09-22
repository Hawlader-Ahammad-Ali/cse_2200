package com.mindmap.dao.impl;

import com.mindmap.dao.KnowledgeDAO;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;
import com.mindmap.model.Origin;
import com.mindmap.model.Tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KnowledgeDAOImpl extends BaseDAO implements KnowledgeDAO {

    private static final String COLS =
            "id, user_id, title, item_type, description, personal_note, category, " +
                    "difficulty, importance, confidence, origin, date_learned, " +
                    "created_at, updated_at";

    @Override
    public KnowledgeItem insert(KnowledgeItem k) throws SQLException {
        String sql = "INSERT INTO knowledge_items " +
                "(user_id, title, item_type, description, personal_note, category, " +
                " difficulty, importance, confidence, origin, date_learned) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, k.getUserId());
            ps.setString(2, k.getTitle());
            ps.setString(3, k.getItemType().name());
            ps.setString(4, k.getDescription());
            ps.setString(5, k.getPersonalNote());
            ps.setString(6, k.getCategory());
            ps.setString(7, k.getDifficulty());
            ps.setString(8, k.getImportance());
            ps.setInt(9, k.getConfidence());
            ps.setString(10, k.getOrigin().name());
            setNullableDate(ps, 11, k.getDateLearned());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) k.setId(keys.getInt(1));
            }
        }
        return findById(k.getId()).orElse(k);
    }

    @Override
    public Optional<KnowledgeItem> findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_items WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                KnowledgeItem k = mapRow(rs);
                loadTags(k);
                return Optional.of(k);
            }
        }
    }

    @Override
    public List<KnowledgeItem> findByUser(int userId) throws SQLException {
        return queryList("SELECT " + COLS + " FROM knowledge_items " +
                "WHERE user_id = ? ORDER BY updated_at DESC", userId);
    }

    @Override
    public List<KnowledgeItem> searchByUser(int userId, String query) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_items WHERE user_id = ? AND (" +
                "LOWER(title) LIKE LOWER(?) OR " +
                "LOWER(IFNULL(description,'')) LIKE LOWER(?) OR " +
                "LOWER(IFNULL(personal_note,'')) LIKE LOWER(?) OR " +
                "LOWER(IFNULL(category,'')) LIKE LOWER(?)" +
                ") ORDER BY updated_at DESC";
        String p = "%" + query.trim() + "%";
        List<KnowledgeItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, p);
            ps.setString(3, p);
            ps.setString(4, p);
            ps.setString(5, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KnowledgeItem k = mapRow(rs);
                    loadTags(k);
                    result.add(k);
                }
            }
        }
        return result;
    }

    @Override
    public List<KnowledgeItem> findByUserAndType(int userId, KnowledgeType type) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_items " +
                "WHERE user_id = ? AND item_type = ? ORDER BY updated_at DESC";
        List<KnowledgeItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KnowledgeItem k = mapRow(rs);
                    loadTags(k);
                    result.add(k);
                }
            }
        }
        return result;
    }

    @Override
    public List<KnowledgeItem> findByUserAndCategory(int userId, String category) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_items " +
                "WHERE user_id = ? AND category = ? ORDER BY updated_at DESC";
        List<KnowledgeItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KnowledgeItem k = mapRow(rs);
                    loadTags(k);
                    result.add(k);
                }
            }
        }
        return result;
    }

    @Override
    public List<KnowledgeItem> findRecent(int userId, int limit) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_items " +
                "WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<KnowledgeItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KnowledgeItem k = mapRow(rs);
                    loadTags(k);
                    result.add(k);
                }
            }
        }
        return result;
    }

    @Override
    public List<String> findDistinctCategories(int userId) throws SQLException {
        String sql = "SELECT DISTINCT category FROM knowledge_items " +
                "WHERE user_id = ? AND category IS NOT NULL AND category <> '' " +
                "ORDER BY category COLLATE NOCASE";
        List<String> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString(1));
            }
        }
        return result;
    }

    @Override
    public void update(KnowledgeItem k) throws SQLException {
        String sql = "UPDATE knowledge_items SET " +
                "title = ?, item_type = ?, description = ?, personal_note = ?, " +
                "category = ?, difficulty = ?, importance = ?, confidence = ?, " +
                "origin = ?, date_learned = ?, updated_at = datetime('now') " +
                "WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, k.getTitle());
            ps.setString(2, k.getItemType().name());
            ps.setString(3, k.getDescription());
            ps.setString(4, k.getPersonalNote());
            ps.setString(5, k.getCategory());
            ps.setString(6, k.getDifficulty());
            ps.setString(7, k.getImportance());
            ps.setInt(8, k.getConfidence());
            ps.setString(9, k.getOrigin().name());
            setNullableDate(ps, 10, k.getDateLearned());
            ps.setInt(11, k.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM knowledge_items WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public int countByUser(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM knowledge_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void setTags(int knowledgeId, List<Integer> tagIds) throws SQLException {
        Connection c = conn();
        boolean oldAuto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM knowledge_tags WHERE knowledge_item_id = ?")) {
                del.setInt(1, knowledgeId);
                del.executeUpdate();
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO knowledge_tags (knowledge_item_id, tag_id) VALUES (?, ?)")) {
                    for (Integer id : tagIds) {
                        ins.setInt(1, knowledgeId);
                        ins.setInt(2, id);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(oldAuto);
        }
    }

    // ------------------------------------------------------------- internals

    private List<KnowledgeItem> queryList(String sql, int userId) throws SQLException {
        List<KnowledgeItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KnowledgeItem k = mapRow(rs);
                    loadTags(k);
                    result.add(k);
                }
            }
        }
        return result;
    }

    private void loadTags(KnowledgeItem k) throws SQLException {
        String sql = "SELECT t.id, t.user_id, t.name, t.color, t.created_at " +
                "FROM knowledge_tags kt JOIN tags t ON t.id = kt.tag_id " +
                "WHERE kt.knowledge_item_id = ? ORDER BY t.name COLLATE NOCASE";
        List<Tag> tags = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, k.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tag t = new Tag();
                    t.setId(rs.getInt("id"));
                    t.setUserId(rs.getInt("user_id"));
                    t.setName(rs.getString("name"));
                    t.setColor(rs.getString("color"));
                    t.setCreatedAt(parseDateTime(rs.getString("created_at")));
                    tags.add(t);
                }
            }
        }
        k.setTags(tags);
    }

    private KnowledgeItem mapRow(ResultSet rs) throws SQLException {
        KnowledgeItem k = new KnowledgeItem();
        k.setId(rs.getInt("id"));
        k.setUserId(rs.getInt("user_id"));
        k.setTitle(rs.getString("title"));
        k.setItemType(KnowledgeType.fromName(rs.getString("item_type")));
        k.setDescription(rs.getString("description"));
        k.setPersonalNote(rs.getString("personal_note"));
        k.setCategory(rs.getString("category"));
        k.setDifficulty(rs.getString("difficulty"));
        k.setImportance(rs.getString("importance"));
        k.setConfidence(rs.getInt("confidence"));
        k.setOrigin(Origin.fromName(rs.getString("origin")));
        String dl = rs.getString("date_learned");
        if (dl != null && !dl.isBlank()) {
            try { k.setDateLearned(LocalDate.parse(dl.substring(0, 10))); }
            catch (Exception ignore) { }
        }
        k.setCreatedAt(parseDateTime(rs.getString("created_at")));
        k.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return k;
    }

    private void setNullableDate(PreparedStatement ps, int idx, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(idx, Types.VARCHAR);
        else           ps.setString(idx, d.toString());
    }
}