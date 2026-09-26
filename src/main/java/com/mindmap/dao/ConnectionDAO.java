package com.mindmap.dao;

import com.mindmap.model.Connection;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ConnectionDAO {

    Connection insert(Connection c) throws SQLException;

    Optional<Connection> findById(int id) throws SQLException;

    List<Connection> findByUser(int userId) throws SQLException;

    /** All edges where knowledgeId is either source or target. */
    List<Connection> findForNode(int knowledgeId) throws SQLException;

    void delete(int id) throws SQLException;

    void deleteBetween(int sourceId, int targetId) throws SQLException;

    int countByUser(int userId) throws SQLException;

    boolean exists(int sourceId, int targetId) throws SQLException;
}