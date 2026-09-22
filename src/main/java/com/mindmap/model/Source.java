package com.mindmap.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Source entity — mirrors the {@code sources} table.
 * Tags are eagerly loaded and held in memory (small N, simple access).
 */
public class Source {

    private int id;
    private int userId;

    private String title;
    private SourceType sourceType = SourceType.CUSTOM;
    private String authorCreator;
    private String description;
    private String url;
    private String coverImage;

    private LocalDateTime dateAdded;
    private LocalDate     dateConsumed;
    private Integer       rating;          // null or 1..5
    private SourceStatus  status = SourceStatus.PLANNED;
    private String        notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private final List<Tag> tags = new ArrayList<>();

    public Source() { }

    // ------------------------------------------------------------- accessors

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public String getAuthorCreator() { return authorCreator; }
    public void setAuthorCreator(String authorCreator) { this.authorCreator = authorCreator; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }

    public LocalDate getDateConsumed() { return dateConsumed; }
    public void setDateConsumed(LocalDate dateConsumed) { this.dateConsumed = dateConsumed; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public SourceStatus getStatus() { return status; }
    public void setStatus(SourceStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tagList) {
        tags.clear();
        if (tagList != null) tags.addAll(tagList);
    }
    public void addTag(Tag tag) { tags.add(tag); }

    /** Comma-separated tag names for quick display in tables. */
    public String getTagString() {
        if (tags.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tags.get(i).getName());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Source{id=" + id + ", title='" + title + "', type=" + sourceType + "}";
    }
}