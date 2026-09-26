package com.mindmap.dao;

import com.mindmap.model.StudySession;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface StudySessionDAO {

    StudySession insert(StudySession s) throws SQLException;

    void finish(int sessionId, int itemsReviewed, int correctCount) throws SQLException;

    /** Distinct dates (most recent first) on which the user completed a session. */
    List<LocalDate> findCompletedDates(int userId) throws SQLException;

    /** Completed sessions on a given date. */
    List<StudySession> findCompletedOn(int userId, LocalDate date) throws SQLException;

    /** Total completed review sessions. */
    int countCompleted(int userId) throws SQLException;
}