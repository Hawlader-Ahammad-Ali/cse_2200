package com.mindmap.dao.impl;

import com.mindmap.dao.TagDAO;
import com.mindmap.model.Tag;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagDAOImpl extends BaseDAO implements TagDAO {

    @Override
    public Tag insert(Tag tag) throws SQLException {
        String sql = "INSERT INTO tags (user_id, name, color) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tag.getUserId());
            ps.setString(2, tag.getName());
            ps.setString(3, tag.getColor() == null ? "#7E57C2" : tag.getColor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) tag.setId(keys.getInt(1));
            }
        }
        return tag;
    }

    @Override
    public Optional<Tag> findById(int id) throws SQLException {
        String sql = "SELECT id, user_id, name, color, created_at FROM tags WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Tag> findByUser(int userId) throws SQLException {
        String sql = "SELECT id, user_id, name, color, created_at " +
                "FROM tags WHERE user_id = ? ORDER BY name COLLATE NOCASE";
        List<Tag> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    @Override
    public Optional<Tag> findByName(int userId, String name) throws SQLException {
        String sql = "SELECT id, user_id, name, color, created_at " +
                "FROM tags WHERE user_id = ? AND LOWER(name) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Tag findOrCreate(int userId, String name) throws SQLException {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new SQLException("Tag name must not be empty");
        }
        Optional<Tag> existing = findByName(userId, trimmed);
        if (existing.isPresent()) return existing.get();

        Tag tag = new Tag(userId, trimmed);
        return insert(tag);
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM tags WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------- internals

    private Tag mapRow(ResultSet rs) throws SQLException {
        Tag t = new Tag();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setName(rs.getString("name"));
        t.setColor(rs.getString("color"));
        t.setCreatedAt(parseDateTime(rs.getString("created_at")));
        return t;
    }
}