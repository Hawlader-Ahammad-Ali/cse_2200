package com.mindmap.dao.impl;

import com.mindmap.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Common helpers shared by all DAO implementations.
 * The one shared SQLite connection is owned by {@link DatabaseManager}.
 */
public abstract class BaseDAO {

    /** Shortcut to the shared JDBC connection. */
    protected Connection conn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    /**
     * Parses a SQLite timestamp ("YYYY-MM-DD HH:MM:SS") into a LocalDateTime.
     * SQLite stores text timestamps with a space, ISO uses 'T'.
     */
    protected LocalDateTime parseDateTime(String sqliteTs) {
        if (sqliteTs == null || sqliteTs.isBlank()) return null;
        try {
            return LocalDateTime.parse(sqliteTs.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    /** ISO timestamp string suitable for SQLite storage. */
    protected String nowIso() {
        return LocalDateTime.now().toString();
    }
}