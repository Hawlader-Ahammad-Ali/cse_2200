package com.mindmap.model;

/**
 * Tracks where a knowledge item came from. Enforced throughout the app:
 * AI can create AI_SUGGESTED items, but only the user can promote them
 * to USER_CONFIRMED. USER_CREATED items are entered directly by the user.
 */
public enum Origin {

    AI_SUGGESTED  ("🤖", "AI Suggested"),
    USER_CONFIRMED("✅", "User Confirmed"),
    USER_CREATED  ("✍",  "User Created");

    private final String icon;
    private final String label;

    Origin(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public String getIcon()  { return icon; }
    public String getLabel() { return label; }
    public String getDisplay() { return icon + "  " + label; }

    public static Origin fromName(String name) {
        if (name == null) return USER_CREATED;
        try { return Origin.valueOf(name); }
        catch (IllegalArgumentException e) { return USER_CREATED; }
    }

    @Override public String toString() { return getDisplay(); }
}