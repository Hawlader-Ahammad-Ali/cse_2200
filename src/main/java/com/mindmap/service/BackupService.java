package com.mindmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mindmap.config.AppConfig;
import com.mindmap.database.DatabaseManager;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/**
 * Full-data backup and restore for the current user.
 *
 * <p>Backup format: a ZIP containing one JSON file per table plus a
 * {@code manifest.json} describing the backup. Ids are preserved on export
 * but remapped on import so collisions with existing rows are impossible.</p>
 *
 * <p><b>Import is destructive</b> for the current user's data — the caller
 * must confirm with the user before invoking {@link #importBackup(Path)}.</p>
 */
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    public static final int BACKUP_FORMAT_VERSION = 1;
    private static final String MANIFEST = "manifest.json";

    /** Tables included in a backup, in dependency order (parent → child). */
    private static final List<String> TABLE_ORDER = List.of(
            "tags",
            "sources",
            "knowledge_items",
            "source_tags",
            "knowledge_tags",
            "source_knowledge",
            "knowledge_connections",
            "flashcards",
            "reviews",
            "review_logs",
            "quiz_questions",
            "quiz_attempts",
            "study_sessions",
            "goals"
    );

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public BackupService() { }

    // =============================================================
    // DTOs
    // =============================================================

    public record BackupInfo(
            int formatVersion,
            String appVersion,
            String username,
            LocalDateTime createdAt,
            Map<String, Integer> tableCounts) { }

    public record ImportResult(
            BackupInfo backup,
            Map<String, Integer> importedCounts) {
        public int totalImported() {
            return importedCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    // =============================================================
    // EXPORT
    // =============================================================

    /**
     * Writes a ZIP backup of the current user's data to {@code destZip}.
     */
    public BackupInfo exportBackup(Path destZip) throws ServiceException {
        int uid = currentUserId();
        User user = AppContext.getInstance().getCurrentUser();
        log.info("Exporting backup for user '{}' to {}", user.getUsername(), destZip);

        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();

        try {
            Connection c = conn();

            // ---- Read each table ----
            for (String table : TABLE_ORDER) {
                List<Map<String, Object>> rows = readTable(c, table, uid);
                data.put(table, rows);
                counts.put(table, rows.size());
            }

            // ---- Write ZIP ----
            Files.createDirectories(destZip.toAbsolutePath().getParent());
            try (FileOutputStream fos = new FileOutputStream(destZip.toFile());
                 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

                // manifest
                Map<String, Object> manifest = new LinkedHashMap<>();
                manifest.put("formatVersion", BACKUP_FORMAT_VERSION);
                manifest.put("appVersion", AppConfig.APP_VERSION);
                manifest.put("username", user.getUsername());
                manifest.put("email", user.getEmail());
                manifest.put("displayName", user.getDisplayName());
                manifest.put("createdAt", LocalDateTime.now().toString());
                manifest.put("tableCounts", counts);

                writeEntry(zos, MANIFEST, MAPPER.writeValueAsBytes(manifest));

                // each table
                for (Map.Entry<String, List<Map<String, Object>>> e : data.entrySet()) {
                    writeEntry(zos, e.getKey() + ".json",
                            MAPPER.writeValueAsBytes(e.getValue()));
                }
            }

            log.info("Backup written: {} ({} tables)", destZip, counts.size());

            return new BackupInfo(
                    BACKUP_FORMAT_VERSION,
                    AppConfig.APP_VERSION,
                    user.getUsername(),
                    LocalDateTime.now(),
                    counts);

        } catch (SQLException e) {
            log.error("Backup export failed (DB)", e);
            throw new ServiceException("Could not read data for backup.", e);
        } catch (IOException e) {
            log.error("Backup export failed (IO)", e);
            throw new ServiceException("Could not write backup file.", e);
        }
    }

    // =============================================================
    // INSPECT (validate before import)
    // =============================================================

    /**
     * Reads only the manifest without modifying anything.
     * Use this to preview a backup before importing.
     */
    public BackupInfo inspectBackup(Path srcZip) throws ServiceException {
        if (!Files.exists(srcZip)) {
            throw new ServiceException("Backup file not found.");
        }

        try (ZipFile zip = new ZipFile(srcZip.toFile())) {
            ZipEntry m = zip.getEntry(MANIFEST);
            if (m == null) {
                throw new ServiceException("This ZIP does not look like a MindMap backup "
                        + "(missing manifest.json).");
            }
            try (InputStream in = zip.getInputStream(m)) {
                Map<String, Object> map = MAPPER.readValue(in, Map.class);

                int formatVersion = toInt(map.get("formatVersion"));
                if (formatVersion > BACKUP_FORMAT_VERSION) {
                    throw new ServiceException("This backup was created by a newer version of "
                            + "MindMap (format v" + formatVersion + "). Please update the app.");
                }

                String appVersion  = String.valueOf(map.getOrDefault("appVersion", "?"));
                String username    = String.valueOf(map.getOrDefault("username", "?"));
                String createdAtS  = String.valueOf(map.getOrDefault("createdAt", ""));

                LocalDateTime createdAt;
                try { createdAt = LocalDateTime.parse(createdAtS); }
                catch (Exception e) { createdAt = LocalDateTime.now(); }

                Map<String, Integer> counts = new LinkedHashMap<>();
                Object countsObj = map.get("tableCounts");
                if (countsObj instanceof Map<?, ?> countsMap) {
                    for (Map.Entry<?, ?> e : countsMap.entrySet()) {
                        counts.put(String.valueOf(e.getKey()),
                                toInt(e.getValue()));
                    }
                }

                return new BackupInfo(formatVersion, appVersion, username, createdAt, counts);
            }
        } catch (IOException e) {
            throw new ServiceException("Could not read backup file: " + e.getMessage(), e);
        }
    }

    // =============================================================
    // IMPORT
    // =============================================================

    /**
     * Restores the current user's data from a backup ZIP.
     *
     * <p><b>Destructive:</b> deletes the current user's existing data first.
     * The caller is responsible for confirming this with the user.</p>
     */
    public ImportResult importBackup(Path srcZip) throws ServiceException {
        int uid = currentUserId();
        log.info("Importing backup from {} for user id={}", srcZip, uid);

        BackupInfo info = inspectBackup(srcZip);

        // ---- Load all JSON into memory ----
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(srcZip.toFile())) {
            for (String table : TABLE_ORDER) {
                ZipEntry entry = zip.getEntry(table + ".json");
                if (entry == null) {
                    data.put(table, Collections.emptyList());
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    List<Map<String, Object>> rows =
                            MAPPER.readValue(in, List.class);
                    data.put(table, rows != null ? rows : Collections.emptyList());
                }
            }
        } catch (IOException e) {
            throw new ServiceException("Could not read backup contents.", e);
        }

        // ---- Perform the import in a transaction ----
        Map<String, Integer> imported = new LinkedHashMap<>();
        Connection c;
        try {
            c = conn();
        } catch (SQLException e) {
            throw new ServiceException("Could not open database.", e);
        }

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);

            // 1. Wipe current user's data (delete in reverse dependency order)
            List<String> deleteOrder = new ArrayList<>(TABLE_ORDER);
            Collections.reverse(deleteOrder);
            for (String table : deleteOrder) {
                wipeUserData(c, table, uid);
            }

            // 2. Insert fresh data with id remapping
            Map<String, Map<Integer, Integer>> idMaps = new HashMap<>();
            for (String table : TABLE_ORDER) {
                List<Map<String, Object>> rows = data.getOrDefault(table, Collections.emptyList());
                Map<Integer, Integer> idMap = new HashMap<>();
                int count = insertRows(c, table, uid, rows, idMaps, idMap);
                idMaps.put(table, idMap);
                imported.put(table, count);
            }

            c.commit();
            log.info("Backup import committed. Imported {} total rows.", sum(imported));

            return new ImportResult(info, imported);

        } catch (Exception e) {
            log.error("Backup import failed — rolling back", e);
            try { c.rollback(); } catch (SQLException ignore) { }
            throw new ServiceException("Import failed: " + e.getMessage(), e);
        } finally {
            try { c.setAutoCommit(originalAutoCommit); } catch (SQLException ignore) { }
        }
    }

    // =============================================================
    // Internal — EXPORT helpers
    // =============================================================

    /** Reads all rows from a table visible to the user. */
    private List<Map<String, Object>> readTable(Connection c, String table, int uid)
            throws SQLException {

        // Some tables have user_id; join tables need special queries.
        String sql = switch (table) {
            case "source_tags" ->
                    "SELECT st.source_id, st.tag_id FROM source_tags st " +
                            "JOIN sources s ON s.id = st.source_id WHERE s.user_id = ?";
            case "knowledge_tags" ->
                    "SELECT kt.knowledge_item_id, kt.tag_id FROM knowledge_tags kt " +
                            "JOIN knowledge_items k ON k.id = kt.knowledge_item_id WHERE k.user_id = ?";
            case "source_knowledge" ->
                    "SELECT sk.source_id, sk.knowledge_item_id, sk.link_type " +
                            "FROM source_knowledge sk " +
                            "JOIN sources s ON s.id = sk.source_id WHERE s.user_id = ?";
            case "review_logs" ->
                    "SELECT rl.id, rl.review_id, rl.quality, rl.reviewed_at " +
                            "FROM review_logs rl " +
                            "JOIN reviews r ON r.id = rl.review_id WHERE r.user_id = ?";
            default ->
                    "SELECT * FROM " + table + " WHERE user_id = ?";
        };

        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        String name = md.getColumnName(i);
                        Object val = rs.getObject(i);
                        // Jackson can't serialize java.sql types directly — convert
                        if (val instanceof java.sql.Timestamp ts) {
                            val = ts.toLocalDateTime().toString();
                        } else if (val instanceof java.sql.Date d) {
                            val = d.toLocalDate().toString();
                        }
                        row.put(name, val);
                    }
                    out.add(row);
                }
            }
        }
        return out;
    }

    private void writeEntry(ZipOutputStream zos, String name, byte[] content) throws IOException {
        ZipEntry e = new ZipEntry(name);
        zos.putNextEntry(e);
        zos.write(content);
        zos.closeEntry();
    }

    // =============================================================
    // Internal — IMPORT helpers
    // =============================================================

    /** Deletes all rows belonging to the user from the given table. */
    private void wipeUserData(Connection c, String table, int uid) throws SQLException {
        String sql = switch (table) {
            case "source_tags" ->
                    "DELETE FROM source_tags WHERE source_id IN " +
                            "(SELECT id FROM sources WHERE user_id = ?)";
            case "knowledge_tags" ->
                    "DELETE FROM knowledge_tags WHERE knowledge_item_id IN " +
                            "(SELECT id FROM knowledge_items WHERE user_id = ?)";
            case "source_knowledge" ->
                    "DELETE FROM source_knowledge WHERE source_id IN " +
                            "(SELECT id FROM sources WHERE user_id = ?)";
            case "review_logs" ->
                    "DELETE FROM review_logs WHERE review_id IN " +
                            "(SELECT id FROM reviews WHERE user_id = ?)";
            case "review_logs_alias" -> "DELETE FROM review_logs WHERE review_id IN " +
                    "(SELECT id FROM reviews WHERE user_id = ?)";
            default ->
                    "DELETE FROM " + table + " WHERE user_id = ?";
        };
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.executeUpdate();
        }
    }

    /**
     * Inserts the given rows, remapping ids and foreign keys.
     * Returns the number of inserted rows.
     */
    private int insertRows(Connection c,
                           String table,
                           int uid,
                           List<Map<String, Object>> rows,
                           Map<String, Map<Integer, Integer>> idMaps,
                           Map<Integer, Integer> thisIdMap) throws SQLException {

        if (rows.isEmpty()) return 0;

        int inserted = 0;
        for (Map<String, Object> row : rows) {
            Map<String, Object> mapped = remapRow(table, row, uid, idMaps);
            insertRow(c, table, mapped, thisIdMap);
            inserted++;
        }
        return inserted;
    }

    /** Applies id remapping and user id override to a single row. */
    private Map<String, Object> remapRow(String table,
                                         Map<String, Object> row,
                                         int uid,
                                         Map<String, Map<Integer, Integer>> idMaps) {
        Map<String, Object> out = new LinkedHashMap<>(row);

        // Override user id for tables that have it
        if (out.containsKey("user_id")) out.put("user_id", uid);

        switch (table) {
            case "sources"          -> { /* user_id already set */ }
            case "tags"             -> { /* user_id already set */ }
            case "knowledge_items"  -> { /* user_id already set */ }
            case "knowledge_connections" -> { /* user_id already set */ }
            case "flashcards"       -> { /* user_id already set */ }
            case "reviews"          -> {
                remapFK(out, "knowledge_item_id", idMaps.get("knowledge_items"));
                remapFK(out, "flashcard_id",      idMaps.get("flashcards"));
            }
            case "quiz_questions"   -> {
                remapFK(out, "knowledge_item_id", idMaps.get("knowledge_items"));
            }
            case "quiz_attempts"    -> {
                remapFK(out, "quiz_question_id", idMaps.get("quiz_questions"));
            }
            case "study_sessions"   -> { /* user_id only */ }
            case "goals"            -> { /* user_id only */ }
            case "source_tags"      -> {
                remapFK(out, "source_id", idMaps.get("sources"));
                remapFK(out, "tag_id",    idMaps.get("tags"));
            }
            case "knowledge_tags"   -> {
                remapFK(out, "knowledge_item_id", idMaps.get("knowledge_items"));
                remapFK(out, "tag_id",            idMaps.get("tags"));
            }
            case "source_knowledge" -> {
                remapFK(out, "source_id",         idMaps.get("sources"));
                remapFK(out, "knowledge_item_id", idMaps.get("knowledge_items"));
            }
            case "review_logs"      -> {
                remapFK(out, "review_id", idMaps.get("reviews"));
            }
        }
        return out;
    }

    /** Replaces a foreign key using the id mapping for the parent table. */
    private void remapFK(Map<String, Object> row, String column,
                         Map<Integer, Integer> parentMap) {
        Object raw = row.get(column);
        if (raw == null) return;

        int oldId = toInt(raw);
        if (parentMap != null && parentMap.containsKey(oldId)) {
            row.put(column, parentMap.get(oldId));
        } else {
            // Orphan FK — set to null so insert doesn't fail on FK violation
            row.put(column, null);
        }
    }

    /** Inserts a single row, dropping its id column so SQLite assigns a new one. */
    private void insertRow(Connection c,
                           String table,
                           Map<String, Object> row,
                           Map<Integer, Integer> thisIdMap) throws SQLException {

        Integer oldId = row.containsKey("id") ? toInt(row.get("id")) : null;

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if ("id".equals(e.getKey())) continue;   // drop id — auto-assigned
            // Skip transient join-table columns that were aliased on export
            columns.add(e.getKey());
            values.add(e.getValue());
        }

        String colList = String.join(", ", columns);
        String placeholders = String.join(", ",
                Collections.nCopies(columns.size(), "?"));

        String sql = "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                Object v = values.get(i);
                if (v == null) ps.setNull(i + 1, Types.NULL);
                else           ps.setObject(i + 1, v);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next() && oldId != null) {
                    thisIdMap.put(oldId, keys.getInt(1));
                }
            }
        }
    }

    // =============================================================
    // Public helpers
    // =============================================================

    /** Suggested backup filename for the current timestamp. */
    public static String defaultBackupFileName() {
        return "MindMap_Backup_" + LocalDateTime.now().format(FILE_TS) + ".zip";
    }

    // =============================================================
    // Tiny helpers
    // =============================================================

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static int sum(Map<String, Integer> m) {
        return m.values().stream().mapToInt(Integer::intValue).sum();
    }

    private Connection conn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    private int currentUserId() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }

    /** Exposed so we can use it from the panel without breaking abstraction. */
    @SuppressWarnings("unused")
    public static List<String> tables() { return TABLE_ORDER; }

    /** Writes text content to a zip entry — helper for future use. */
    @SuppressWarnings("unused")
    private static byte[] utf8(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }
}