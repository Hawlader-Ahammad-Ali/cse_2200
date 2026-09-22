package com.mindmap.dao.impl;

import com.mindmap.dao.SourceKnowledgeDAO;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;
import com.mindmap.model.Origin;
import com.mindmap.model.Source;
import com.mindmap.model.SourceStatus;
import com.mindmap.model.SourceType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SourceKnowledgeDAOImpl extends BaseDAO implements SourceKnowledgeDAO {

    private static final String DEFAULT_LINK_TYPE = "LEARNED_FROM";

    @Override
    public void link(int sourceId, int knowledgeId, String linkType) throws SQLException {
        String type = (linkType == null || linkType.isBlank()) ? DEFAULT_LINK_TYPE : linkType;

        // INSERT OR IGNORE keeps this idempotent — safe to call repeatedly.
        String sql = "INSERT OR IGNORE INTO source_knowledge " +
                "(source_id, knowledge_item_id, link_type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sourceId);
            ps.setInt(2, knowledgeId);
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    @Override
    public void unlink(int sourceId, int knowledgeId) throws SQLException {
        String sql = "DELETE FROM source_knowledge WHERE source_id = ? AND knowledge_item_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sourceId);
            ps.setInt(2, knowledgeId);
            ps.executeUpdate();
        }
    }

    @Override
    public void unlinkAllForSource(int sourceId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM source_knowledge WHERE source_id = ?")) {
            ps.setInt(1, sourceId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean exists(int sourceId, int knowledgeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM source_knowledge WHERE source_id = ? AND knowledge_item_id = ? LIMIT 1")) {
            ps.setInt(1, sourceId);
            ps.setInt(2, knowledgeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<KnowledgeItem> findKnowledgeBySource(int sourceId) throws SQLException {
        // Join source_knowledge with knowledge_items; aggregate tags separately.
        String sql = "SELECT k.id, k.user_id, k.title, k.item_type, k.description, " +
                "k.personal_note, k.category, k.difficulty, k.importance, k.confidence, " +
                "k.origin, k.date_learned, k.created_at, k.updated_at " +
                "FROM source_knowledge sk " +
                "JOIN knowledge_items k ON k.id = sk.knowledge_item_id " +
                "WHERE sk.source_id = ? ORDER BY k.title COLLATE NOCASE";

        List<KnowledgeItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sourceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapKnowledgeRow(rs));
            }
        }
        // Load tags for each (kept simple for clarity; small N expected)
        for (KnowledgeItem k : items) {
            k.setTags(loadTagsFor(k.getId()));
        }
        return items;
    }

    @Override
    public List<Source> findSourcesByKnowledge(int knowledgeId) throws SQLException {
        String sql = "SELECT s.id, s.user_id, s.title, s.source_type, s.author_creator, " +
                "s.description, s.url, s.cover_image, s.date_added, s.date_consumed, " +
                "s.rating, s.status, s.notes, s.created_at, s.updated_at " +
                "FROM source_knowledge sk " +
                "JOIN sources s ON s.id = sk.source_id " +
                "WHERE sk.knowledge_item_id = ? ORDER BY s.date_added DESC";

        List<Source> sources = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, knowledgeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sources.add(mapSourceRow(rs));
            }
        }
        return sources;
    }

    // ------------------------------------------------------------- internals

    private List<com.mindmap.model.Tag> loadTagsFor(int knowledgeId) throws SQLException {
        String sql = "SELECT t.id, t.user_id, t.name, t.color, t.created_at " +
                "FROM knowledge_tags kt JOIN tags t ON t.id = kt.tag_id " +
                "WHERE kt.knowledge_item_id = ? ORDER BY t.name COLLATE NOCASE";
        List<com.mindmap.model.Tag> tags = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, knowledgeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.mindmap.model.Tag t = new com.mindmap.model.Tag();
                    t.setId(rs.getInt("id"));
                    t.setUserId(rs.getInt("user_id"));
                    t.setName(rs.getString("name"));
                    t.setColor(rs.getString("color"));
                    t.setCreatedAt(parseDateTime(rs.getString("created_at")));
                    tags.add(t);
                }
            }
        }
        return tags;
    }

    private KnowledgeItem mapKnowledgeRow(ResultSet rs) throws SQLException {
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

    private Source mapSourceRow(ResultSet rs) throws SQLException {
        Source s = new Source();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setTitle(rs.getString("title"));
        s.setSourceType(SourceType.fromName(rs.getString("source_type")));
        s.setAuthorCreator(rs.getString("author_creator"));
        s.setDescription(rs.getString("description"));
        s.setUrl(rs.getString("url"));
        s.setCoverImage(rs.getString("cover_image"));
        s.setDateAdded(parseDateTime(rs.getString("date_added")));
        String consumed = rs.getString("date_consumed");
        if (consumed != null && !consumed.isBlank()) {
            try { s.setDateConsumed(LocalDate.parse(consumed.substring(0, 10))); }
            catch (Exception ignore) { }
        }
        int rating = rs.getInt("rating");
        s.setRating(rs.wasNull() ? null : rating);
        s.setStatus(SourceStatus.fromName(rs.getString("status")));
        s.setNotes(rs.getString("notes"));
        s.setCreatedAt(parseDateTime(rs.getString("created_at")));
        s.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return s;
    }
}