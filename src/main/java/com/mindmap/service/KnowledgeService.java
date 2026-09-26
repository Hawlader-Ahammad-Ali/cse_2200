package com.mindmap.service;

import com.mindmap.dao.KnowledgeDAO;
import com.mindmap.dao.SourceKnowledgeDAO;
import com.mindmap.dao.TagDAO;
import com.mindmap.dao.impl.KnowledgeDAOImpl;
import com.mindmap.dao.impl.SourceKnowledgeDAOImpl;
import com.mindmap.dao.impl.TagDAOImpl;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;
import com.mindmap.model.Origin;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business logic for knowledge items.
 * Also owns source ↔ knowledge linking (via {@link SourceKnowledgeDAO}).
 */
public class KnowledgeService {

    private final KnowledgeDAO       knowledgeDAO;
    private final TagDAO             tagDAO;
    private final SourceKnowledgeDAO linkDAO;

    public KnowledgeService() {
        this.knowledgeDAO = new KnowledgeDAOImpl();
        this.tagDAO       = new TagDAOImpl();
        this.linkDAO      = new SourceKnowledgeDAOImpl();
    }

    /** Test constructor. */
    public KnowledgeService(KnowledgeDAO knowledgeDAO,
                            TagDAO tagDAO,
                            SourceKnowledgeDAO linkDAO) {
        this.knowledgeDAO = knowledgeDAO;
        this.tagDAO       = tagDAO;
        this.linkDAO      = linkDAO;
    }

    // ------------------------------------------------------------- reads

    public List<KnowledgeItem> getAll() throws ServiceException {
        try { return knowledgeDAO.findByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load knowledge items.", e); }
    }

    public List<KnowledgeItem> search(String query) throws ServiceException {
        if (query == null || query.isBlank()) return getAll();
        try { return knowledgeDAO.searchByUser(uid(), query.trim()); }
        catch (SQLException e) { throw new ServiceException("Search failed.", e); }
    }

    public List<KnowledgeItem> filterByType(KnowledgeType type) throws ServiceException {
        try { return knowledgeDAO.findByUserAndType(uid(), type); }
        catch (SQLException e) { throw new ServiceException("Could not filter.", e); }
    }

    public List<KnowledgeItem> filterByCategory(String category) throws ServiceException {
        try { return knowledgeDAO.findByUserAndCategory(uid(), category); }
        catch (SQLException e) { throw new ServiceException("Could not filter.", e); }
    }

    public List<KnowledgeItem> getRecent(int limit) throws ServiceException {
        try { return knowledgeDAO.findRecent(uid(), limit); }
        catch (SQLException e) { throw new ServiceException("Could not load recent items.", e); }
    }

    public Optional<KnowledgeItem> getById(int id) throws ServiceException {
        try { return knowledgeDAO.findById(id); }
        catch (SQLException e) { throw new ServiceException("Could not load item.", e); }
    }

    public int countAll() throws ServiceException {
        try { return knowledgeDAO.countByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count items.", e); }
    }

    public List<String> getCategories() throws ServiceException {
        try { return knowledgeDAO.findDistinctCategories(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load categories.", e); }
    }

    public List<Tag> getAllTags() throws ServiceException {
        try { return tagDAO.findByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load tags.", e); }
    }

    /** "Where did I learn this?" — sources linked to a knowledge item. */
    public List<Source> getSourcesFor(int knowledgeId) throws ServiceException {
        try { return linkDAO.findSourcesByKnowledge(knowledgeId); }
        catch (SQLException e) { throw new ServiceException("Could not load sources.", e); }
    }

    /** Related knowledge for a source — used by SourceDetailView. */
    public List<KnowledgeItem> getKnowledgeForSource(int sourceId) throws ServiceException {
        try { return linkDAO.findKnowledgeBySource(sourceId); }
        catch (SQLException e) { throw new ServiceException("Could not load linked knowledge.", e); }
    }

    // ------------------------------------------------------------- writes

    /**
     * Creates a knowledge item, links tags, and links it to the given sources.
     * @param tagNames   may be null/empty
     * @param sourceIds  may be null/empty
     */
    public KnowledgeItem create(KnowledgeItem item,
                                List<String> tagNames,
                                List<Integer> sourceIds) throws ServiceException {
        validate(item);
        item.setUserId(uid());
        if (item.getOrigin() == null) item.setOrigin(Origin.USER_CREATED);

        try {
            KnowledgeItem saved = knowledgeDAO.insert(item);
            List<Integer> tagIds = ensureTags(saved.getUserId(), tagNames);
            knowledgeDAO.setTags(saved.getId(), tagIds);

            if (sourceIds != null) {
                for (Integer sid : sourceIds) {
                    linkDAO.link(sid, saved.getId(), "LEARNED_FROM");
                }
            }

            // Phase 11: enroll the new item in spaced repetition
            try {
                ReviewService reviewService = new ReviewService();
                reviewService.enrollKnowledgeItem(saved.getId());
            } catch (ServiceException e) {
                // Non-fatal — log and continue; item is still created
                org.slf4j.LoggerFactory.getLogger(KnowledgeService.class)
                        .warn("Could not enroll item in reviews: {}", e.getMessage());
            }

            return knowledgeDAO.findById(saved.getId()).orElse(saved);
        } catch (SQLException e) {
            throw new ServiceException("Could not save knowledge item.", e);
        }
    }

    public KnowledgeItem update(KnowledgeItem item,
                                List<String> tagNames,
                                List<Integer> sourceIds) throws ServiceException {
        validate(item);
        if (item.getId() <= 0) throw new ServiceException("Invalid knowledge item.");

        try {
            knowledgeDAO.update(item);
            List<Integer> tagIds = ensureTags(item.getUserId(), tagNames);
            knowledgeDAO.setTags(item.getId(), tagIds);

            // Replace source links: unlink all, then link the current set.
            // (Simple and correct; safe because the number of links per item is small.)
            List<Source> currentSources = linkDAO.findSourcesByKnowledge(item.getId());
            for (Source s : currentSources) {
                linkDAO.unlink(s.getId(), item.getId());
            }
            if (sourceIds != null) {
                for (Integer sid : sourceIds) {
                    linkDAO.link(sid, item.getId(), "LEARNED_FROM");
                }
            }
            return knowledgeDAO.findById(item.getId()).orElse(item);
        } catch (SQLException e) {
            throw new ServiceException("Could not update knowledge item.", e);
        }
    }

    public void delete(int id) throws ServiceException {
        try { knowledgeDAO.delete(id); }
        catch (SQLException e) { throw new ServiceException("Could not delete item.", e); }
    }

    /** Promotes an AI-suggested item to USER_CONFIRMED. */
    public void confirmAIItem(KnowledgeItem item) throws ServiceException {
        if (item.getOrigin() != Origin.AI_SUGGESTED) return;
        item.setOrigin(Origin.USER_CONFIRMED);
        try { knowledgeDAO.update(item); }
        catch (SQLException e) { throw new ServiceException("Could not confirm item.", e); }
    }

    // ------------------------------------------------------------- helpers

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }

    private List<Integer> ensureTags(int userId, List<String> tagNames) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        if (tagNames == null || tagNames.isEmpty()) return ids;

        Set<String> unique = new LinkedHashSet<>();
        for (String raw : tagNames) {
            if (raw == null) continue;
            String t = raw.trim();
            if (!t.isEmpty()) unique.add(t);
        }
        for (String name : unique) {
            Tag tag = tagDAO.findOrCreate(userId, name);
            ids.add(tag.getId());
        }
        return ids;
    }

    private void validate(KnowledgeItem k) throws ServiceException {
        if (k == null) throw new ServiceException("No knowledge item provided.");
        if (k.getTitle() == null || k.getTitle().isBlank())
            throw new ServiceException("Title is required.");
        if (k.getTitle().length() > 300)
            throw new ServiceException("Title is too long (max 300 characters).");
        if (k.getItemType() == null) k.setItemType(KnowledgeType.CONCEPT);
        if (k.getConfidence() < 1 || k.getConfidence() > 5)
            throw new ServiceException("Confidence must be between 1 and 5.");
        if (k.getDifficulty() == null) k.setDifficulty("MEDIUM");
        if (k.getImportance() == null) k.setImportance("MEDIUM");
    }
}