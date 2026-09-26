package com.mindmap.model;

import java.time.LocalDateTime;

/**
 * A directed edge in the knowledge graph — mirrors {@code knowledge_connections}.
 */
public class Connection {

    private int id;
    private int userId;
    private int sourceItemId;
    private int targetItemId;
    private RelationshipType relationshipType = RelationshipType.RELATED_TO;
    private String notes;
    private double weight = 1.0;
    private LocalDateTime createdAt;

    public Connection() { }

    public Connection(int sourceItemId, int targetItemId, RelationshipType type) {
        this.sourceItemId = sourceItemId;
        this.targetItemId = targetItemId;
        this.relationshipType = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getSourceItemId() { return sourceItemId; }
    public void setSourceItemId(int sourceItemId) { this.sourceItemId = sourceItemId; }

    public int getTargetItemId() { return targetItemId; }
    public void setTargetItemId(int targetItemId) { this.targetItemId = targetItemId; }

    public RelationshipType getRelationshipType() { return relationshipType; }
    public void setRelationshipType(RelationshipType t) { this.relationshipType = t; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Connection{" + sourceItemId + " -[" + relationshipType + "]-> " + targetItemId + "}";
    }
}