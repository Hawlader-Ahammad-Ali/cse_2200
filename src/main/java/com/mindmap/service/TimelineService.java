package com.mindmap.service;

import com.mindmap.database.DatabaseManager;
import com.mindmap.model.TimelineEvent;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a merged, chronological feed of the user's learning activity.
 *
 * <p>Each entity (sources, knowledge items, reviews, quizzes, flashcards)
 * is queried separately, mapped to a {@link TimelineEvent}, and merged
 * in-memory. Simpler than a giant SQL UNION and easier to maintain.</p>
 */
public class TimelineService {

    private static final Logger log = LoggerFactory.getLogger(TimelineService.class);

    public TimelineService() { }

    // ------------------------------------------------------------- main

    /**
     * Fetches timeline events for the current user, newest first.
     *
     * @param from  optional lower bound (inclusive), may be null
     * @param to    optional upper bound (inclusive), may be null
     * @param kind  optional filter, may be null for "all kinds"
     */
    public List<TimelineEvent> getTimeline(LocalDate from, LocalDate to, TimelineEvent.Kind kind)
            throws ServiceException {
        int uid = uid();
        List<TimelineEvent> events = new ArrayList<>();

        try {
            events.addAll(fetchSources(uid, from, to, kind));
            events.addAll(fetchKnowledge(uid, from, to, kind));
            events.addAll(fetchTakeaways(uid, from, to, kind));
            events.addAll(fetchReviews(uid, from, to, kind));
            events.addAll(fetchQuizSessions(uid, from, to, kind));
            events.addAll(fetchFlashcards(uid, from, to, kind));
        } catch (SQLException e) {
            log.error("Could not build timeline", e);
            throw new ServiceException("Could not load timeline.", e);
        }

        events.sort(Comparator.comparing(TimelineEvent::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return events;
    }

    // ------------------------------------------------------------- per-entity fetches

    private List<TimelineEvent> fetchSources(int uid, LocalDate from, LocalDate to,
                                             TimelineEvent.Kind kind) throws SQLException {
        if (kind != null && kind != TimelineEvent.Kind.SOURCE_ADDED) return List.of();

        String sql = "SELECT id, title, source_type, date_added, date_consumed " +
                "FROM sources WHERE user_id = ?";
        List<TimelineEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Prefer the "consumed" date if present, else "added"
                    String ts = rs.getString("date_consumed");
                    if (ts == null || ts.isBlank()) ts = rs.getString("date_added");
                    LocalDateTime dt = parse(ts);
                    if (dt == null || !inRange(dt, from, to)) continue;

                    TimelineEvent e = new TimelineEvent();
                    e.setKind(TimelineEvent.Kind.SOURCE_ADDED);
                    e.setTimestamp(dt);
                    e.setTitle(rs.getString("title"));
                    e.setSubtitle("Source added");
                    e.setSourceId(rs.getInt("id"));
                    out.add(e);
                }
            }
        }
        return out;
    }

    private List<TimelineEvent> fetchKnowledge(int uid, LocalDate from, LocalDate to,
                                               TimelineEvent.Kind kind) throws SQLException {
        if (kind != null && kind != TimelineEvent.Kind.KNOWLEDGE_CREATED
                && kind != TimelineEvent.Kind.KNOWLEDGE_UPDATED) return List.of();

        String sql = "SELECT id, title, category, item_type, origin, created_at, updated_at " +
                "FROM knowledge_items WHERE user_id = ?";
        List<TimelineEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime created = parse(rs.getString("created_at"));
                    LocalDateTime updated = parse(rs.getString("updated_at"));

                    // Created event
                    if (created != null && inRange(created, from, to)) {
                        TimelineEvent e = new TimelineEvent();
                        e.setKind(TimelineEvent.Kind.KNOWLEDGE_CREATED);
                        e.setTimestamp(created);
                        e.setTitle(rs.getString("title"));
                        String cat = rs.getString("category");
                        e.setSubtitle(cat == null || cat.isBlank()
                                ? "Knowledge added"
                                : "Knowledge added · " + cat);
                        e.setKnowledgeItemId(rs.getInt("id"));
                        out.add(e);
                    }

                    // Updated event — only if it's meaningfully later than created
                    if (updated != null && created != null
                            && updated.isAfter(created.plusMinutes(2))
                            && inRange(updated, from, to)) {
                        TimelineEvent e = new TimelineEvent();
                        e.setKind(TimelineEvent.Kind.KNOWLEDGE_UPDATED);
                        e.setTimestamp(updated);
                        e.setTitle(rs.getString("title"));
                        e.setSubtitle("Knowledge updated");
                        e.setKnowledgeItemId(rs.getInt("id"));
                        out.add(e);
                    }
                }
            }
        }
        return out;
    }

    private List<TimelineEvent> fetchTakeaways(int uid, LocalDate from, LocalDate to,
                                               TimelineEvent.Kind kind) throws SQLException {
        if (kind != null && kind != TimelineEvent.Kind.TAKEAWAY_WRITTEN) return List.of();

        String sql = "SELECT id, title, personal_note, updated_at " +
                "FROM knowledge_items " +
                "WHERE user_id = ? AND personal_note IS NOT NULL AND personal_note <> ''";
        List<TimelineEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime dt = parse(rs.getString("updated_at"));
                    if (dt == null || !inRange(dt, from, to)) continue;

                    String note = rs.getString("personal_note");
                    String preview = note.length() > 60 ? note.substring(0, 57) + "…" : note;

                    TimelineEvent e = new TimelineEvent();
                    e.setKind(TimelineEvent.Kind.TAKEAWAY_WRITTEN);
                    e.setTimestamp(dt);
                    e.setTitle(rs.getString("title"));
                    e.setSubtitle("Takeaway: " + preview);
                    e.setKnowledgeItemId(rs.getInt("id"));
                    out.add(e);
                }
            }
        }
        return out;
    }

    private List<TimelineEvent> fetchReviews(int uid, LocalDate from, LocalDate to,
                                             TimelineEvent.Kind kind) throws SQLException {
        if (kind != null && kind != TimelineEvent.Kind.REVIEW_COMPLETED) return List.of();

        // Group review_logs by day for a compact timeline
        String sql = "SELECT DATE(rl.reviewed_at) AS d, COUNT(*) AS n, " +
                "SUM(CASE WHEN rl.quality >= 3 THEN 1 ELSE 0 END) AS good " +
                "FROM review_logs rl JOIN reviews r ON r.id = rl.review_id " +
                "WHERE r.user_id = ? " +
                "GROUP BY DATE(rl.reviewed_at) " +
                "ORDER BY d DESC";
        List<TimelineEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dStr = rs.getString("d");
                    if (dStr == null) continue;
                    LocalDate d;
                    try { d = LocalDate.parse(dStr); }
                    catch (Exception ignore) { continue; }

                    if (!inRange(d.atStartOfDay(), from, to)) continue;

                    int n = rs.getInt("n");
                    int good = rs.getInt("good");

                    TimelineEvent e = new TimelineEvent();
                    e.setKind(TimelineEvent.Kind.REVIEW_COMPLETED);
                    e.setTimestamp(d.atTime(LocalTime.NOON));
                    e.setTitle(n + " item" + (n == 1 ? "" : "s") + " reviewed");
                    e.setSubtitle(good + " / " + n + " recalled correctly");
                    out.add(e);
                }
            }
        }
        return out;
    }

    private List<TimelineEvent> fetchQuizSessions(int uid, LocalDate from, LocalDate to,
                                                  TimelineEvent.Kind kind) throws SQLException {
        if (kind != null && kind != TimelineEvent.Kind.QUIZ_COMPLETED) return List.of();

        String sql = "SELECT id, end_time, items_reviewed, correct_count " +
                "FROM study_sessions " +
                "WHERE user_id = ? AND session_type = 'QUIZ' AND end_time IS NOT NULL";
        List<TimelineEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime dt = parse(rs.getString("end_time"));
                    if (dt == null || !inRange(dt, from, to)) continue;

                    int n = rs.getInt("items_reviewed");
                    int c = rs.getInt("correct_count");

                    TimelineEvent e = new TimelineEvent();
                    e.setKind(TimelineEvent.Kind.QUIZ_COMPLETED);
                    e.setTimestamp(dt);
                    e.setTitle("Quiz session");
                    e.setSubtitle(c + " / " + n + " correct");
                    out.add(e);
                }
            }
        }
        return out;
    }

    private List<TimelineEvent> fetchFlashcards(int uid, LocalDate from, LocalDate to,
                                                TimelineEvent.Kind kind) throws SQLException {
        if (kind != null && kind != TimelineEvent.Kind.FLASHCARD_CREATED) return List.of();

        String sql = "SELECT id, question, origin, created_at " +
                "FROM flashcards WHERE user_id = ?";
        List<TimelineEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime dt = parse(rs.getString("created_at"));
                    if (dt == null || !inRange(dt, from, to)) continue;

                    String q = rs.getString("question");
                    String preview = q == null ? "" : (q.length() > 60 ? q.substring(0, 57) + "…" : q);

                    TimelineEvent e = new TimelineEvent();
                    e.setKind(TimelineEvent.Kind.FLASHCARD_CREATED);
                    e.setTimestamp(dt);
                    e.setTitle("Flashcard created");
                    e.setSubtitle(preview);
                    e.setFlashcardId(rs.getInt("id"));
                    out.add(e);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------- helpers

    private static boolean inRange(LocalDateTime dt, LocalDate from, LocalDate to) {
        if (from != null && dt.toLocalDate().isBefore(from)) return false;
        if (to   != null && dt.toLocalDate().isAfter(to))    return false;
        return true;
    }

    private static LocalDateTime parse(String sqliteTs) {
        if (sqliteTs == null || sqliteTs.isBlank()) return null;
        try { return LocalDateTime.parse(sqliteTs.replace(' ', 'T')); }
        catch (Exception e) { return null; }
    }

    private Connection conn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }
}