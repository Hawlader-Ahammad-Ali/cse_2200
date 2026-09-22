package com.mindmap.model;

/**
 * Type of a knowledge item. Enum name is stored in the DB.
 */
public enum KnowledgeType {

    CONCEPT  ("💡", "Concept"),
    FACT     ("📌", "Fact"),
    SKILL    ("🛠", "Skill"),
    PRINCIPLE("⚖", "Principle"),
    IDEA     ("✨", "Idea"),
    LESSON   ("📖", "Lesson"),
    INSIGHT  ("🔍", "Insight"),
    QUESTION ("❓", "Question");

    private final String icon;
    private final String label;

    KnowledgeType(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public String getIcon()  { return icon; }
    public String getLabel() { return label; }
    public String getDisplay() { return icon + "  " + label; }

    public static KnowledgeType fromName(String name) {
        if (name == null) return CONCEPT;
        try { return KnowledgeType.valueOf(name); }
        catch (IllegalArgumentException e) { return CONCEPT; }
    }

    @Override public String toString() { return getDisplay(); }
}