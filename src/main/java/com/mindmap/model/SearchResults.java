package com.mindmap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified search response. A single query is fanned out to
 * sources, knowledge items, and tags; each result set is grouped here.
 */
public class SearchResults {

    private final String query;
    private List<Source>        sources   = new ArrayList<>();
    private List<KnowledgeItem> knowledge = new ArrayList<>();
    private List<Tag>           tags      = new ArrayList<>();

    public SearchResults(String query) {
        this.query = query == null ? "" : query;
    }

    public String getQuery() { return query; }

    public List<Source>        getSources()   { return sources; }
    public List<KnowledgeItem> getKnowledge() { return knowledge; }
    public List<Tag>           getTags()      { return tags; }

    public void setSources(List<Source> s)        { this.sources = s == null ? new ArrayList<>() : s; }
    public void setKnowledge(List<KnowledgeItem> k){ this.knowledge = k == null ? new ArrayList<>() : k; }
    public void setTags(List<Tag> t)              { this.tags = t == null ? new ArrayList<>() : t; }

    /** Distinct result count (takeaways are a subset of knowledge, not counted separately). */
    public int getTotalCount() {
        return sources.size() + knowledge.size() + tags.size();
    }

    public boolean isEmpty() {
        return getTotalCount() == 0;
    }

    /**
     * True if the knowledge item's personal takeaway matched the query
     * (used to show a "Takeaway" badge on the result row).
     */
    public boolean matchedInTakeaway(KnowledgeItem item) {
        if (item.getPersonalNote() == null || query.isBlank()) return false;
        return item.getPersonalNote().toLowerCase().contains(query.toLowerCase());
    }
}