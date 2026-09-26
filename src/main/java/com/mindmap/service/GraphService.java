package com.mindmap.service;

import com.mindmap.dao.ConnectionDAO;
import com.mindmap.dao.KnowledgeDAO;
import com.mindmap.dao.SourceKnowledgeDAO;
import com.mindmap.dao.impl.ConnectionDAOImpl;
import com.mindmap.dao.impl.KnowledgeDAOImpl;
import com.mindmap.dao.impl.SourceKnowledgeDAOImpl;
import com.mindmap.model.Connection;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.RelationshipType;
import com.mindmap.model.Source;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Graph operations: build the full graph, manage connections, and
 * detect cross-source concepts.
 */
public class GraphService {

    private final ConnectionDAO      connectionDAO;
    private final KnowledgeDAO       knowledgeDAO;
    private final SourceKnowledgeDAO linkDAO;

    public GraphService() {
        this.connectionDAO = new ConnectionDAOImpl();
        this.knowledgeDAO  = new KnowledgeDAOImpl();
        this.linkDAO       = new SourceKnowledgeDAOImpl();
    }

    /** Test constructor. */
    public GraphService(ConnectionDAO connectionDAO,
                        KnowledgeDAO knowledgeDAO,
                        SourceKnowledgeDAO linkDAO) {
        this.connectionDAO = connectionDAO;
        this.knowledgeDAO  = knowledgeDAO;
        this.linkDAO       = linkDAO;
    }

    // ------------------------------------------------------------- graph

    public List<KnowledgeItem> getNodes() throws ServiceException {
        try { return knowledgeDAO.findByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load graph nodes.", e); }
    }

    public List<Connection> getEdges() throws ServiceException {
        try { return connectionDAO.findByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load graph edges.", e); }
    }

    public int countEdges() throws ServiceException {
        try { return connectionDAO.countByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count edges.", e); }
    }

    /** All connections touching the given node. */
    public List<Connection> connectionsFor(int knowledgeId) throws ServiceException {
        try { return connectionDAO.findForNode(knowledgeId); }
        catch (SQLException e) { throw new ServiceException("Could not load connections.", e); }
    }

    // ------------------------------------------------------------- writes

    public Connection createConnection(int sourceId,
                                       int targetId,
                                       RelationshipType type,
                                       String notes) throws ServiceException {
        if (sourceId == targetId) {
            throw new ServiceException("An item cannot connect to itself.");
        }
        if (type == null) type = RelationshipType.RELATED_TO;

        int uid = uid();
        try {
            if (connectionDAO.exists(sourceId, targetId)) {
                throw new ServiceException("These items are already connected.");
            }
            Connection c = new Connection(sourceId, targetId, type);
            c.setUserId(uid);
            c.setNotes(notes);
            return connectionDAO.insert(c);
        } catch (SQLException e) {
            throw new ServiceException("Could not create connection.", e);
        }
    }

    public void deleteConnection(int connectionId) throws ServiceException {
        try { connectionDAO.delete(connectionId); }
        catch (SQLException e) { throw new ServiceException("Could not delete connection.", e); }
    }

    public void deleteConnectionBetween(int sourceId, int targetId) throws ServiceException {
        try { connectionDAO.deleteBetween(sourceId, targetId); }
        catch (SQLException e) { throw new ServiceException("Could not delete connection.", e); }
    }

    // ------------------------------------------------------------- neighbors

    public Set<Integer> neighborIds(int knowledgeId) throws ServiceException {
        Set<Integer> result = new HashSet<>();
        for (Connection c : connectionsFor(knowledgeId)) {
            if (c.getSourceItemId() == knowledgeId) result.add(c.getTargetItemId());
            else result.add(c.getSourceItemId());
        }
        return result;
    }

    // ------------------------------------------------------------- cross-source

    /**
     * Returns a map of knowledge item id → number of distinct sources it's
     * linked to, for items linked to at least 2 sources.
     */
    public Map<Integer, Integer> findCrossSourceItems() throws ServiceException {
        int uid = uid();
        List<KnowledgeItem> all;
        try { all = knowledgeDAO.findByUser(uid); }
        catch (SQLException e) { throw new ServiceException("Could not load knowledge.", e); }

        Map<Integer, Integer> result = new HashMap<>();
        for (KnowledgeItem k : all) {
            try {
                List<Source> sources = linkDAO.findSourcesByKnowledge(k.getId());
                if (sources.size() >= 2) {
                    result.put(k.getId(), sources.size());
                }
            } catch (SQLException e) {
                // skip this node, keep going
            }
        }
        return result;
    }

    // ------------------------------------------------------------- helpers

    public List<Source> sourcesFor(int knowledgeId) throws ServiceException {
        try { return linkDAO.findSourcesByKnowledge(knowledgeId); }
        catch (SQLException e) { throw new ServiceException("Could not load sources.", e); }
    }

    public Optional<KnowledgeItem> getById(int id) throws ServiceException {
        try { return knowledgeDAO.findById(id); }
        catch (SQLException e) { throw new ServiceException("Could not load item.", e); }
    }

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }
}