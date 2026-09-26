package com.mindmap.service;

import com.mindmap.dao.KnowledgeDAO;
import com.mindmap.dao.SourceDAO;
import com.mindmap.dao.TagDAO;
import com.mindmap.dao.impl.KnowledgeDAOImpl;
import com.mindmap.dao.impl.SourceDAOImpl;
import com.mindmap.dao.impl.TagDAOImpl;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.SearchResults;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;

import java.sql.SQLException;
import java.util.List;

/**
 * Unified global search across sources, knowledge items, and tags.
 * Each DAO already has a case-insensitive search for its own entity; this
 * service just fans out and groups results.
 */
public class SearchService {

    private final SourceDAO    sourceDAO;
    private final KnowledgeDAO knowledgeDAO;
    private final TagDAO       tagDAO;

    public SearchService() {
        this.sourceDAO    = new SourceDAOImpl();
        this.knowledgeDAO = new KnowledgeDAOImpl();
        this.tagDAO       = new TagDAOImpl();
    }

    /** Test constructor. */
    public SearchService(SourceDAO sourceDAO,
                         KnowledgeDAO knowledgeDAO,
                         TagDAO tagDAO) {
        this.sourceDAO = sourceDAO;
        this.knowledgeDAO = knowledgeDAO;
        this.tagDAO = tagDAO;
    }

    /**
     * Runs a global search. Returns an empty {@link SearchResults}
     * (never null) when the query is blank.
     */
    public SearchResults search(String query) throws ServiceException {
        SearchResults results = new SearchResults(query);
        if (query == null || query.isBlank()) return results;

        int uid = currentUserId();
        String q = query.trim();
        String lower = q.toLowerCase();

        try {
            List<Source> sources = sourceDAO.searchByUser(uid, q);
            results.setSources(sources);

            List<KnowledgeItem> knowledge = knowledgeDAO.searchByUser(uid, q);
            results.setKnowledge(knowledge);

            // Tags — filter the user's tags by name substring (no DAO search method needed)
            List<Tag> allTags = tagDAO.findByUser(uid);
            List<Tag> matchingTags = allTags.stream()
                    .filter(t -> t.getName() != null
                            && t.getName().toLowerCase().contains(lower))
                    .toList();
            results.setTags(matchingTags);

            return results;

        } catch (SQLException e) {
            throw new ServiceException("Search failed.", e);
        }
    }

    // ------------------------------------------------------------- helpers

    private int currentUserId() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }
}