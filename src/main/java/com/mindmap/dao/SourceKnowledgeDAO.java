package com.mindmap.dao;

import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Source;

import java.sql.SQLException;
import java.util.List;

/**
 * Manages the {@code source_knowledge} M:N link table.
 * Powers "Where did I learn this?" and "Related Knowledge".
 */
public interface SourceKnowledgeDAO {

    /** Creates the link if not already present. */
    void link(int sourceId, int knowledgeId, String linkType) throws SQLException;

    void unlink(int sourceId, int knowledgeId) throws SQLException;

    /** Removes all links for a source (used when a source is deleted). */
    void unlinkAllForSource(int sourceId) throws SQLException;

    boolean exists(int sourceId, int knowledgeId) throws SQLException;

    /** All knowledge items linked to a given source. */
    List<KnowledgeItem> findKnowledgeBySource(int sourceId) throws SQLException;

    /** All sources linked to a given knowledge item ("Where did I learn this?"). */
    List<Source> findSourcesByKnowledge(int knowledgeId) throws SQLException;
}