package com.mindmap.service;

import com.mindmap.database.DatabaseManager;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only analytics over the user's data.
 * All queries are simple aggregations — no caching needed for typical dataset sizes.
 */
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    public AnalyticsService() { }

    // =============================================================
    // DTOs
    // =============================================================

    public record DashboardStats(
            int totalSources,
            int totalKnowledge,
            int totalConnections,
            int reviewsDue,
            int reviewedToday,
            int currentStreak,
            int totalFlashcards,
            int totalQuizQuestions) { }

    public record TopicAccuracy(String label, int attempts, int correct) {
        public double getAccuracy() {
            return attempts == 0 ? 0.0 : (double) correct / attempts;
        }
    }

    public record CrossSourceConcept(String title, int sourceCount) { }

    // =============================================================
    // Dashboard stats
    // =============================================================

    public DashboardStats getDashboardStats() throws ServiceException {
        int uid = uid();
        try {
            int sources = countTable("sources", uid);
            int knowledge = countTable("knowledge_items", uid);
            int connections = countTable("knowledge_connections", uid);
            int flashcards = countTable("flashcards", uid);
            int quizQuestions = countTable("quiz_questions", uid);

            int due = scalarInt(
                    "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND next_review_date <= ?",
                    uid, LocalDate.now().toString());

            int reviewedToday = scalarInt(
                    "SELECT COUNT(*) FROM review_logs rl " +
                            "JOIN reviews r ON r.id = rl.review_id " +
                            "WHERE r.user_id = ? AND DATE(rl.reviewed_at) = ?",
                    uid, LocalDate.now().toString());

            int streak = computeStreak(uid);

            return new DashboardStats(sources, knowledge, connections,
                    due, reviewedToday, streak, flashcards, quizQuestions);

        } catch (SQLException e) {
            throw new ServiceException("Could not load dashboard stats.", e);
        }
    }

    // =============================================================
    // Charts data
    // =============================================================

    /** Category → count of knowledge items. Sorted descending. */
    public Map<String, Integer> getCategoryDistribution() throws ServiceException {
        int uid = uid();
        String sql = "SELECT COALESCE(NULLIF(category, ''), 'Uncategorized') AS cat, " +
                "COUNT(*) AS n FROM knowledge_items WHERE user_id = ? " +
                "GROUP BY cat ORDER BY n DESC";
        Map<String, Integer> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("cat"), rs.getInt("n"));
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not load categories.", e);
        }
        return out;
    }

    /**
     * Sources added per month for the last 12 months (including empty months).
     */
    public Map<YearMonth, Integer> getSourcesOverTime() throws ServiceException {
        return groupByMonth("sources", "date_added", uid());
    }

    /**
     * Knowledge items created per month for the last 12 months.
     */
    public Map<YearMonth, Integer> getKnowledgeOverTime() throws ServiceException {
        return groupByMonth("knowledge_items", "created_at", uid());
    }

    /**
     * Activity events per day for the last 30 days.
     * Activity = knowledge created + reviews logged + quiz attempts.
     */
    public Map<LocalDate, Integer> getActivityLast30Days() throws ServiceException {
        int uid = uid();
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();

        // Initialize all 30 days to 0
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) counts.put(today.minusDays(i), 0);

        try {
            // Knowledge created
            accumulateDaily(counts,
                    "SELECT DATE(created_at) AS d, COUNT(*) AS n FROM knowledge_items " +
                            "WHERE user_id = ? AND created_at >= datetime('now', '-30 days') " +
                            "GROUP BY DATE(created_at)", uid);

            // Reviews logged
            accumulateDaily(counts,
                    "SELECT DATE(rl.reviewed_at) AS d, COUNT(*) AS n FROM review_logs rl " +
                            "JOIN reviews r ON r.id = rl.review_id " +
                            "WHERE r.user_id = ? AND rl.reviewed_at >= datetime('now', '-30 days') " +
                            "GROUP BY DATE(rl.reviewed_at)", uid);

            // Quiz attempts
            accumulateDaily(counts,
                    "SELECT DATE(attempted_at) AS d, COUNT(*) AS n FROM quiz_attempts " +
                            "WHERE user_id = ? AND attempted_at >= datetime('now', '-30 days') " +
                            "GROUP BY DATE(attempted_at)", uid);

        } catch (SQLException e) {
            throw new ServiceException("Could not load activity.", e);
        }
        return counts;
    }

    // =============================================================
    // Weak / strong topics
    // =============================================================

    /**
     * Weak topics: knowledge items with quiz attempts averaging <= 50% correct
     * (at least 2 attempts), OR items with low average confidence.
     */
    public List<TopicAccuracy> getWeakTopics(int limit) throws ServiceException {
        int uid = uid();
        String sql = "SELECT k.title, COUNT(qa.id) AS attempts, " +
                "SUM(qa.is_correct) AS correct " +
                "FROM quiz_attempts qa " +
                "JOIN quiz_questions q ON q.id = qa.quiz_question_id " +
                "JOIN knowledge_items k ON k.id = q.knowledge_item_id " +
                "WHERE qa.user_id = ? " +
                "GROUP BY k.id HAVING attempts >= 2 AND (CAST(correct AS REAL) / attempts) <= 0.5 " +
                "ORDER BY (CAST(correct AS REAL) / attempts) ASC, attempts DESC LIMIT ?";
        List<TopicAccuracy> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TopicAccuracy(
                            rs.getString("title"),
                            rs.getInt("attempts"),
                            rs.getInt("correct")));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not compute weak topics.", e);
        }
        return out;
    }

    public List<TopicAccuracy> getStrongTopics(int limit) throws ServiceException {
        int uid = uid();
        String sql = "SELECT k.title, COUNT(qa.id) AS attempts, " +
                "SUM(qa.is_correct) AS correct " +
                "FROM quiz_attempts qa " +
                "JOIN quiz_questions q ON q.id = qa.quiz_question_id " +
                "JOIN knowledge_items k ON k.id = q.knowledge_item_id " +
                "WHERE qa.user_id = ? " +
                "GROUP BY k.id HAVING attempts >= 2 AND (CAST(correct AS REAL) / attempts) >= 0.8 " +
                "ORDER BY (CAST(correct AS REAL) / attempts) DESC, attempts DESC LIMIT ?";
        List<TopicAccuracy> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new TopicAccuracy(
                            rs.getString("title"),
                            rs.getInt("attempts"),
                            rs.getInt("correct")));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not compute strong topics.", e);
        }
        return out;
    }

    // =============================================================
    // Cross-source concepts
    // =============================================================

    public List<CrossSourceConcept> getTopCrossSourceConcepts(int limit) throws ServiceException {
        int uid = uid();
        String sql = "SELECT k.title, COUNT(DISTINCT sk.source_id) AS n " +
                "FROM knowledge_items k " +
                "JOIN source_knowledge sk ON sk.knowledge_item_id = k.id " +
                "WHERE k.user_id = ? " +
                "GROUP BY k.id HAVING n >= 2 " +
                "ORDER BY n DESC, k.title ASC LIMIT ?";
        List<CrossSourceConcept> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CrossSourceConcept(
                            rs.getString("title"),
                            rs.getInt("n")));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not load cross-source concepts.", e);
        }
        return out;
    }

    // =============================================================
    // Interest signals
    // =============================================================

    /**
     * "What am I learning?" — top 3 categories by number of knowledge items.
     * Purely data-driven; never makes psychological claims.
     */
    public List<String> getInterestSignals(int limit) throws ServiceException {
        Map<String, Integer> dist = getCategoryDistribution();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : dist.entrySet()) {
            if (e.getKey() == null || "Uncategorized".equals(e.getKey())) continue;
            out.add(e.getKey());
            if (out.size() >= limit) break;
        }
        return out;
    }

    // =============================================================
    // Monthly growth table
    // =============================================================

    public record MonthlyGrowth(YearMonth month, int sources, int knowledge, int reviews) { }

    public List<MonthlyGrowth> getMonthlyGrowth(int months) throws ServiceException {
        int uid = uid();
        Map<YearMonth, Integer> sources = getSourcesOverTime();
        Map<YearMonth, Integer> knowledge = getKnowledgeOverTime();
        Map<YearMonth, Integer> reviews = new LinkedHashMap<>();

        try {
            String sql = "SELECT strftime('%Y-%m', rl.reviewed_at) AS m, COUNT(*) AS n " +
                    "FROM review_logs rl JOIN reviews r ON r.id = rl.review_id " +
                    "WHERE r.user_id = ? AND rl.reviewed_at >= date('now', '-12 months') " +
                    "GROUP BY m";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, uid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String m = rs.getString("m");
                        if (m == null) continue;
                        try {
                            YearMonth ym = YearMonth.parse(m);
                            reviews.put(ym, rs.getInt("n"));
                        } catch (Exception ignore) { }
                    }
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not load review history.", e);
        }

        List<MonthlyGrowth> out = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            out.add(new MonthlyGrowth(
                    ym,
                    sources.getOrDefault(ym, 0),
                    knowledge.getOrDefault(ym, 0),
                    reviews.getOrDefault(ym, 0)));
        }
        return out;
    }

    // =============================================================
    // Helpers
    // =============================================================

    private Map<YearMonth, Integer> groupByMonth(String table, String dateCol, int uid)
            throws ServiceException {
        Map<YearMonth, Integer> out = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) out.put(current.minusMonths(i), 0);

        String sql = "SELECT strftime('%Y-%m', " + dateCol + ") AS m, COUNT(*) AS n " +
                "FROM " + table + " WHERE user_id = ? " +
                "AND " + dateCol + " >= date('now', '-12 months') GROUP BY m";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String m = rs.getString("m");
                    if (m == null) continue;
                    try {
                        YearMonth ym = YearMonth.parse(m);
                        if (out.containsKey(ym)) out.put(ym, rs.getInt("n"));
                    } catch (Exception ignore) { }
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not group by month.", e);
        }
        return out;
    }

    private void accumulateDaily(Map<LocalDate, Integer> map,
                                 String sql, int uid) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String d = rs.getString("d");
                    if (d == null) continue;
                    try {
                        LocalDate date = LocalDate.parse(d);
                        if (map.containsKey(date)) {
                            map.put(date, map.get(date) + rs.getInt("n"));
                        }
                    } catch (Exception ignore) { }
                }
            }
        }
    }

    private int computeStreak(int uid) throws SQLException {
        String sql = "SELECT DISTINCT DATE(end_time) AS d FROM study_sessions " +
                "WHERE user_id = ? AND end_time IS NOT NULL ORDER BY d DESC";
        List<LocalDate> dates = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String d = rs.getString("d");
                    if (d != null) {
                        try { dates.add(LocalDate.parse(d)); } catch (Exception ignore) { }
                    }
                }
            }
        }
        if (dates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate cursor;
        if (dates.get(0).equals(today)) cursor = today;
        else if (dates.get(0).equals(today.minusDays(1))) cursor = today.minusDays(1);
        else return 0;

        int streak = 0;
        for (LocalDate d : dates) {
            if (d.equals(cursor)) { streak++; cursor = cursor.minusDays(1); }
            else if (d.isBefore(cursor)) break;
        }
        return streak;
    }

    private int countTable(String table, int uid) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE user_id = ?")) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int scalarInt(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer n) ps.setInt(i + 1, n);
                else                                ps.setString(i + 1, String.valueOf(params[i]));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Connection conn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }

    /** Utility: format "yyyy-MM" as "MMM yyyy" for axis labels. */
    public static String prettyMonth(YearMonth ym) {
        return ym.format(DateTimeFormatter.ofPattern("MMM yy"));
    }
}