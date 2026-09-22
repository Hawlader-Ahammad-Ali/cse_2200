package com.mindmap.model;

import java.time.LocalDateTime;

/**
 * Tag entity — belongs to a user, used by both sources and knowledge items.
 */
public class Tag {

    private int id;
    private int userId;
    private String name;
    private String color = "#7E57C2";
    private LocalDateTime createdAt;

    public Tag() { }

    public Tag(int userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag other = (Tag) o;
        return id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}