package com.mindmap.model;

/**
 * Types of sources a user can add. Enum name is stored in the DB;
 * icon + label are used for display only.
 */
public enum SourceType {

    MOVIE       ("🎬", "Movie"),
    SERIES      ("📺", "Series"),
    ANIME       ("🌸", "Anime"),
    DOCUMENTARY ("🎥", "Documentary"),
    BOOK        ("📖", "Book"),
    ARTICLE     ("📰", "Article"),
    PAPER       ("📄", "Research Paper"),
    VIDEO       ("▶", "Video"),
    PODCAST     ("🎙", "Podcast"),
    LECTURE     ("🎓", "Lecture"),
    COURSE      ("📚", "Course"),
    PROJECT     ("🛠", "Project"),
    EXPERIENCE  ("🌱", "Experience"),
    CONVERSATION("💬", "Conversation"),
    CUSTOM      ("📌", "Custom");

    private final String icon;
    private final String label;

    SourceType(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public String getIcon()  { return icon; }
    public String getLabel() { return label; }

    /** "🎬 Movie" — for combo boxes and labels. */
    public String getDisplay() { return icon + "  " + label; }

    /** Safe parser — returns CUSTOM on unknown values. */
    public static SourceType fromName(String name) {
        if (name == null) return CUSTOM;
        try {
            return SourceType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }

    @Override public String toString() { return getDisplay(); }
}