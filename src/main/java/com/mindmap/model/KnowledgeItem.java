package com.mindmap.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A knowledge item: concept, fact, skill, principle, insight, etc.
 * Mirrors the {@code knowledge_items} table. Tags and source links are
 * transient (loaded eagerly by the DAO on demand).
 */
public class KnowledgeItem {

    private int id;
    private int userId;

    private String title;
    private KnowledgeType itemType = KnowledgeType.CONCEPT;
    private String description;
    private String personalNote;         // user's takeaway
    private String category;             // e.g. Business, Science
    private String difficulty = "MEDIUM"; // EASY | MEDIUM | HARD
    private String importance = "MEDIUM"; // LOW | MEDIUM | HIGH
    private int    confidence = 3;        // 1..5
    private Origin origin = Origin.USER_CREATED;
    private LocalDate dateLearned;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private final List<Tag> tags = new ArrayList<>();

    public KnowledgeItem() { }

    // ------------------------------------------------------------- accessors

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public KnowledgeType getItemType() { return itemType; }
    public void setItemType(KnowledgeType itemType) { this.itemType = itemType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPersonalNote() { return personalNote; }
    public void setPersonalNote(String personalNote) { this.personalNote = personalNote; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }

    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }

    public LocalDate getDateLearned() { return dateLearned; }
    public void setDateLearned(LocalDate dateLearned) { this.dateLearned = dateLearned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> list) {
        tags.clear();
        if (list != null) tags.addAll(list);
    }
    public void addTag(Tag t) { tags.add(t); }

    public String getTagString() {
        if (tags.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tags.get(i).getName());
        }
        return sb.toString();
    }

    /** Icon-prefixed label combining type + title, used in lists. */
    public String getDisplayTitle() {
        return itemType.getIcon() + "  " + title;
    }

    @Override
    public String toString() {
        return "KnowledgeItem{id=" + id + ", title='" + title + "', type=" + itemType + "}";
    }
}