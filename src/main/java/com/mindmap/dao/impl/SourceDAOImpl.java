package com.mindmap.dao.impl;

import com.mindmap.dao.SourceDAO;
import com.mindmap.model.Source;
import com.mindmap.model.SourceStatus;
import com.mindmap.model.SourceType;
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

public class SourceDAOImpl extends BaseDAO implements SourceDAO {

    private static final String COLS =
            "id, user_id, title, source_type, author_creator, description, url, " +
                    "cover_image, date_added, date_consumed, rating, status, notes, " +
                    "created_at, updated_at";

    @Override
    public Source insert(Source s) throws SQLException {
        String sql = "INSERT INTO sources " +
                "(user_id, title, source_type, author_creator, description, url, " +
                " cover_image, date_consumed, rating, status, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getTitle());
            ps.setString(3, s.getSourceType().name());
            ps.setString(4, s.getAuthorCreator());
            ps.setString(5, s.getDescription());
            ps.setString(6, s.getUrl());
            ps.setString(7, s.getCoverImage());
            setNullableDate(ps, 8, s.getDateConsumed());
            setNullableInt(ps, 9, s.getRating());
            ps.setString(10, s.getStatus().name());
            ps.setString(11, s.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
        }
        // Load the server-set timestamps
        return findById(s.getId()).orElse(s);
    }

    @Override
    public Optional<Source> findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM sources WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Source s = mapRow(rs);
                loadTags(s);
                return Optional.of(s);
            }
        }
    }

    @Override
    public List<Source> findByUser(int userId) throws SQLException {
        return queryList("SELECT " + COLS + " FROM sources WHERE user_id = ? " +
                "ORDER BY date_added DESC", userId);
    }

    @Override
    public List<Source> searchByUser(int userId, String query) throws SQLException {
        String sql = "SELECT " + COLS + " FROM sources WHERE user_id = ? AND (" +
                "LOWER(title) LIKE LOWER(?) OR " +
                "LOWER(IFNULL(author_creator,'')) LIKE LOWER(?) OR " +
                "LOWER(IFNULL(description,'')) LIKE LOWER(?) OR " +
                "LOWER(IFNULL(notes,'')) LIKE LOWER(?)" +
                ") ORDER BY date_added DESC";
        String pattern = "%" + query.trim() + "%";
        List<Source> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Source s = mapRow(rs);
                    loadTags(s);
                    result.add(s);
                }
            }
        }
        return result;
    }

    @Override
    public List<Source> findByUserAndType(int userId, SourceType type) throws SQLException {
        String sql = "SELECT " + COLS + " FROM sources " +
                "WHERE user_id = ? AND source_type = ? ORDER BY date_added DESC";
        List<Source> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Source s = mapRow(rs);
                    loadTags(s);
                    result.add(s);
                }
            }
        }
        return result;
    }

    @Override
    public List<Source> findByUserAndStatus(int userId, SourceStatus status) throws SQLException {
        String sql = "SELECT " + COLS + " FROM sources " +
                "WHERE user_id = ? AND status = ? ORDER BY date_added DESC";
        List<Source> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Source s = mapRow(rs);
                    loadTags(s);
                    result.add(s);
                }
            }
        }
        return result;
    }

    @Override
    public List<Source> findRecent(int userId, int limit) throws SQLException {
        String sql = "SELECT " + COLS + " FROM sources " +
                "WHERE user_id = ? ORDER BY date_added DESC LIMIT ?";
        List<Source> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Source s = mapRow(rs);
                    loadTags(s);
                    result.add(s);
                }
            }
        }
        return result;
    }

    @Override
    public void update(Source s) throws SQLException {
        String sql = "UPDATE sources SET " +
                "title = ?, source_type = ?, author_creator = ?, description = ?, " +
                "url = ?, cover_image = ?, date_consumed = ?, rating = ?, " +
                "status = ?, notes = ?, updated_at = datetime('now') " +
                "WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getSourceType().name());
            ps.setString(3, s.getAuthorCreator());
            ps.setString(4, s.getDescription());
            ps.setString(5, s.getUrl());
            ps.setString(6, s.getCoverImage());
            setNullableDate(ps, 7, s.getDateConsumed());
            setNullableInt(ps, 8, s.getRating());
            ps.setString(9, s.getStatus().name());
            ps.setString(10, s.getNotes());
            ps.setInt(11, s.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM sources WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public int countByUser(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM sources WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void setTags(int sourceId, List<Integer> tagIds) throws SQLException {
        Connection c = conn();
        boolean oldAuto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM source_tags WHERE source_id = ?")) {
                del.setInt(1, sourceId);
                del.executeUpdate();
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO source_tags (source_id, tag_id) VALUES (?, ?)")) {
                    for (Integer tagId : tagIds) {
                        ins.setInt(1, sourceId);
                        ins.setInt(2, tagId);
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

    private List<Source> queryList(String sql, int userId) throws SQLException {
        List<Source> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Source s = mapRow(rs);
                    loadTags(s);
                    result.add(s);
                }
            }
        }
        return result;
    }

    private void loadTags(Source s) throws SQLException {
        String sql = "SELECT t.id, t.user_id, t.name, t.color, t.created_at " +
                "FROM source_tags st JOIN tags t ON t.id = st.tag_id " +
                "WHERE st.source_id = ? ORDER BY t.name COLLATE NOCASE";
        List<Tag> tags = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, s.getId());
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
        s.setTags(tags);
    }

    private Source mapRow(ResultSet rs) throws SQLException {
        Source s = new Source();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setTitle(rs.getString("title"));
        s.setSourceType(SourceType.fromName(rs.getString("source_type")));
        s.setAuthorCreator(rs.getString("author_creator"));
        s.setDescription(rs.getString("description"));
        s.setUrl(rs.getString("url"));
        s.setCoverImage(rs.getString("cover_image"));

        String added = rs.getString("date_added");
        if (added != null) s.setDateAdded(parseDateTime(added));

        String consumed = rs.getString("date_consumed");
        if (consumed != null && !consumed.isBlank()) {
            // consumed is stored as ISO date or ISO datetime; try both
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

    private void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else           ps.setInt(idx, v);
    }

    private void setNullableDate(PreparedStatement ps, int idx, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(idx, Types.VARCHAR);
        else           ps.setString(idx, d.toString());
    }
}