package com.mindmap.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@code /db/schema.sql} once, on first launch.
 *
 * <p>Idempotent: if the {@code users} table already exists, initialization
 * is skipped. {@code schema_version} tracks the current version so future
 * migrations (Phase 3+) can be applied safely.</p>
 */
public final class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private SchemaInitializer() { /* utility class */ }

    /**
     * Initializes the schema if not already present.
     * @throws IllegalStateException if the schema file is missing or SQL fails.
     */
    public static void initialize() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            if (isInitialized(conn)) {
                int version = readSchemaVersion(conn);
                log.info("Database schema already initialized (version {}).", version);
                applyMigrations(conn);
                return;
            }

            log.info("Initializing database schema (first launch)...");
            String sql = readSchemaFile();
            List<String> statements = splitSqlStatements(sql);

            conn.setAutoCommit(false);
            try {
                int executed = 0;
                for (String stmt : statements) {
                    if (stmt.isBlank()) continue;
                    try (Statement st = conn.createStatement()) {
                        st.execute(stmt);
                        executed++;
                    }
                }
                conn.commit();
                log.info("Schema initialized: {} statements executed.", executed);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            log.error("Database schema initialization failed", e);
            throw new IllegalStateException("Could not initialize database schema", e);
        } catch (IOException e) {
            log.error("Could not read schema.sql", e);
            throw new IllegalStateException("schema.sql is missing from resources", e);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isInitialized(Connection conn) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='users'";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    private static int readSchemaVersion(Connection conn) {
        String sql = "SELECT MAX(version) FROM schema_version";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.warn("Could not read schema_version — assuming 0", e);
            return 0;
        }
    }

    private static String readSchemaFile() throws IOException {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IOException("Resource not found on classpath: " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }
    }

    /**
     * Splits a SQL script into individual statements.
     *
     * <p>Handles:
     * <ul>
     *   <li>{@code --} line comments (dropped)</li>
     *   <li>single-quoted strings — semicolons inside are ignored</li>
     * </ul>
     * Good enough for our hand-written schema; not a general SQL parser.</p>
     */
    static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean inLineComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    current.append('\n');
                }
                continue;
            }

            if (!inString && c == '-' && next == '-') {
                inLineComment = true;
                i++; // skip the second '-'
                continue;
            }

            if (c == '\'') {
                // handle escaped quote '' inside a string
                if (inString && next == '\'') {
                    current.append("''");
                    i++;
                    continue;
                }
                inString = !inString;
                current.append(c);
                continue;
            }

            if (!inString && c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) statements.add(stmt);
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) statements.add(tail);
        return statements;
    }

    private static void applyMigrations(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS notes (
                id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id            INTEGER NOT NULL,
                title              TEXT NOT NULL,
                body               TEXT,
                attached_source_id INTEGER,
                created_at         TEXT NOT NULL DEFAULT (datetime('now')),
                updated_at         TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(attached_source_id) REFERENCES sources(id) ON DELETE SET NULL
            );
            CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
            CREATE INDEX IF NOT EXISTS idx_notes_source_id ON notes(attached_source_id);
        """;
        try (Statement st = conn.createStatement()) {
            for (String stmt : splitSqlStatements(sql)) {
                if (!stmt.isBlank()) st.execute(stmt);
            }
        }
    }
}
