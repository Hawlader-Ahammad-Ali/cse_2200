package com.mindmap.dao.impl;

import com.mindmap.dao.ConnectionDAO;
import com.mindmap.model.Connection;
import com.mindmap.model.RelationshipType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConnectionDAOImpl extends BaseDAO implements ConnectionDAO {

    private static final String COLS =
            "id, user_id, source_item_id, target_item_id, relationship_type, " +
                    "notes, weight, created_at";

    @Override
    public Connection insert(Connection c) throws SQLException {
        String sql = "INSERT INTO knowledge_connections " +
                "(user_id, source_item_id, target_item_id, relationship_type, notes, weight) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getUserId());
            ps.setInt(2, c.getSourceItemId());
            ps.setInt(3, c.getTargetItemId());
            ps.setString(4, c.getRelationshipType().name());
            ps.setString(5, c.getNotes());
            ps.setDouble(6, c.getWeight());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
        }
        return findById(c.getId()).orElse(c);
    }

    @Override
    public Optional<Connection> findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_connections WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Connection> findByUser(int userId) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_connections WHERE user_id = ?";
        List<Connection> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<Connection> findForNode(int knowledgeId) throws SQLException {
        String sql = "SELECT " + COLS + " FROM knowledge_connections " +
                "WHERE source_item_id = ? OR target_item_id = ?";
        List<Connection> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, knowledgeId);
            ps.setInt(2, knowledgeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM knowledge_connections WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteBetween(int sourceId, int targetId) throws SQLException {
        String sql = "DELETE FROM knowledge_connections " +
                "WHERE (source_item_id = ? AND target_item_id = ?) " +
                "   OR (source_item_id = ? AND target_item_id = ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sourceId); ps.setInt(2, targetId);
            ps.setInt(3, targetId); ps.setInt(4, sourceId);
            ps.executeUpdate();
        }
    }

    @Override
    public int countByUser(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM knowledge_connections WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public boolean exists(int sourceId, int targetId) throws SQLException {
        String sql = "SELECT 1 FROM knowledge_connections " +
                "WHERE (source_item_id = ? AND target_item_id = ?) " +
                "   OR (source_item_id = ? AND target_item_id = ?) LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sourceId); ps.setInt(2, targetId);
            ps.setInt(3, targetId); ps.setInt(4, sourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ------------------------------------------------------------- internals

    private Connection mapRow(ResultSet rs) throws SQLException {
        Connection c = new Connection();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setSourceItemId(rs.getInt("source_item_id"));
        c.setTargetItemId(rs.getInt("target_item_id"));
        c.setRelationshipType(RelationshipType.fromName(rs.getString("relationship_type")));
        c.setNotes(rs.getString("notes"));
        c.setWeight(rs.getDouble("weight"));
        c.setCreatedAt(parseDateTime(rs.getString("created_at")));
        return c;
    }
}