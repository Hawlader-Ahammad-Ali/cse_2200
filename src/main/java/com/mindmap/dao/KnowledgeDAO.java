package com.mindmap.dao;

import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface KnowledgeDAO {

    KnowledgeItem insert(KnowledgeItem item) throws SQLException;

    Optional<KnowledgeItem> findById(int id) throws SQLException;

    List<KnowledgeItem> findByUser(int userId) throws SQLException;

    List<KnowledgeItem> searchByUser(int userId, String query) throws SQLException;

    List<KnowledgeItem> findByUserAndType(int userId, KnowledgeType type) throws SQLException;

    List<KnowledgeItem> findByUserAndCategory(int userId, String category) throws SQLException;

    List<KnowledgeItem> findRecent(int userId, int limit) throws SQLException;

    /** Distinct categories for a user, sorted alphabetically. Used in filter combos. */
    List<String> findDistinctCategories(int userId) throws SQLException;

    void update(KnowledgeItem item) throws SQLException;

    void delete(int id) throws SQLException;

    int countByUser(int userId) throws SQLException;

    /** Replaces the item's tag links with the given tag ids. */
    void setTags(int knowledgeId, List<Integer> tagIds) throws SQLException;
}