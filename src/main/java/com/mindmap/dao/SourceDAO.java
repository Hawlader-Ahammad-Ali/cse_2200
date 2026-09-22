package com.mindmap.dao;

import com.mindmap.model.Source;
import com.mindmap.model.SourceStatus;
import com.mindmap.model.SourceType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SourceDAO {

    /** Inserts a source and populates the generated id + date_added. */
    Source insert(Source source) throws SQLException;

    Optional<Source> findById(int id) throws SQLException;

    List<Source> findByUser(int userId) throws SQLException;

    /** Case-insensitive search on title, author_creator, description, notes. */
    List<Source> searchByUser(int userId, String query) throws SQLException;

    List<Source> findByUserAndType(int userId, SourceType type) throws SQLException;

    List<Source> findByUserAndStatus(int userId, SourceStatus status) throws SQLException;

    /** Most recently added sources first. */
    List<Source> findRecent(int userId, int limit) throws SQLException;

    void update(Source source) throws SQLException;

    void delete(int id) throws SQLException;

    int countByUser(int userId) throws SQLException;

    /** Replaces the source's tag links with the given tag ids. */
    void setTags(int sourceId, List<Integer> tagIds) throws SQLException;
}