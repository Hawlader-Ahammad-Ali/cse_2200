package com.mindmap.dao;

import com.mindmap.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Data-access interface for the {@code users} table.
 * Implementations live in {@code com.mindmap.dao.impl}.
 */
public interface UserDAO {

    /** Inserts a new user and populates the generated id. */
    User insert(User user) throws SQLException;

    Optional<User> findById(int id) throws SQLException;

    Optional<User> findByUsername(String username) throws SQLException;

    Optional<User> findByEmail(String email) throws SQLException;

    /** Case-insensitive lookup by username OR email — for login. */
    Optional<User> findByUsernameOrEmail(String usernameOrEmail) throws SQLException;

    boolean usernameExists(String username) throws SQLException;

    boolean emailExists(String email) throws SQLException;

    void updateLastLogin(int userId) throws SQLException;
}