package com.mindmap.database;

import com.mindmap.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite connection for the whole application.
 *
 * <p>SQLite is designed for a single writer; using one shared connection
 * (with synchronized access) avoids file-locking headaches on desktop.
 * Every DAO in later phases will call {@link #getConnection()}.</p>
 *
 * <p>Call {@link #close()} on app shutdown (see Main#stop).</p>
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;

    private final String jdbcUrl;
    private Connection connection;

    private DatabaseManager(String dbFilePath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
    }

    /** Lazily creates the singleton, using the OS-standard DB path. */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager(AppPaths.getDatabaseFile().toString());
        }
        return instance;
    }

    /** Test-only factory for in-memory databases. */
    static synchronized DatabaseManager createForTest(String jdbcUrl) {
        DatabaseManager m = new DatabaseManager(":memory:");
        m.jdbcUrlOverride = jdbcUrl;
        return m;
    }

    /** Override field used only by createForTest. */
    private String jdbcUrlOverride;

    /**
     * Returns the shared connection, creating it on first call.
     * Applies safe pragmas: foreign keys ON, WAL journal.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = (jdbcUrlOverride != null) ? jdbcUrlOverride : jdbcUrl;
            connection = DriverManager.getConnection(url);
            configureConnection(connection);
            log.info("SQLite connection opened: {}", url);
        }
        return connection;
    }

    private void configureConnection(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous  = NORMAL");
        }
    }

    /**
     * Closes the shared connection. Safe to call multiple times.
     */
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    log.info("SQLite connection closed.");
                }
            } catch (SQLException e) {
                log.warn("Error while closing SQLite connection", e);
            } finally {
                connection = null;
            }
        }
    }
}