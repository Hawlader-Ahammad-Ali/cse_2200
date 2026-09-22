package com.mindmap.dao.impl;

import com.mindmap.dao.UserDAO;
import com.mindmap.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDAOImpl extends BaseDAO implements UserDAO {

    private static final String SELECT_COLS =
            "id, username, email, password_hash, salt, display_name, " +
                    "avatar_path, created_at, updated_at, last_login";

    @Override
    public User insert(User user) throws SQLException {
        String sql = "INSERT INTO users " +
                "(username, email, password_hash, salt, display_name, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getSalt());
            ps.setString(5, user.getDisplayName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
        return user;
    }

    @Override
    public Optional<User> findById(int id) throws SQLException {
        return queryOne("SELECT " + SELECT_COLS + " FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        return queryOne("SELECT " + SELECT_COLS + " FROM users WHERE username = ?", username);
    }

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        return queryOne("SELECT " + SELECT_COLS + " FROM users WHERE email = ?", email);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        String sql = "SELECT " + SELECT_COLS + " FROM users " +
                "WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean usernameExists(String username) throws SQLException {
        return exists("SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1", username);
    }

    @Override
    public boolean emailExists(String email) throws SQLException {
        return exists("SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1", email);
    }

    @Override
    public void updateLastLogin(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET last_login = datetime('now'), updated_at = datetime('now') " +
                        "WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------- internals

    private Optional<User> queryOne(String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (param instanceof Integer i) ps.setInt(1, i);
            else                           ps.setString(1, String.valueOf(param));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private boolean exists(String sql, String param) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setDisplayName(rs.getString("display_name"));
        u.setAvatarPath(rs.getString("avatar_path"));
        u.setCreatedAt(parseDateTime(rs.getString("created_at")));
        u.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        u.setLastLogin(parseDateTime(rs.getString("last_login")));
        return u;
    }
}