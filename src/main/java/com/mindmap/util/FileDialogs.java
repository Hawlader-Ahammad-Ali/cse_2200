package com.mindmap.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * JavaFX file chooser helpers.
 */
public final class FileDialogs {

    private FileDialogs() { }

    /** Save dialog with a default filename and extension filter. */
    public static File chooseSaveFile(String title, String defaultFileName, String extension) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialFileName(defaultFileName);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        extension.toUpperCase() + " Files", "*." + extension));
        return fc.showSaveDialog(activeWindow());
    }

    /** Open dialog with extension filter. */
    public static File chooseOpenFile(String title, String extension) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        extension.toUpperCase() + " Files", "*." + extension));
        return fc.showOpenDialog(activeWindow());
    }

    /**
     * Makes a filename safe for the OS: strips illegal characters,
     * truncates to a reasonable length, and never returns an empty string.
     */
    public static String sanitizeFileName(String s) {
        if (s == null || s.isBlank()) return "MindMap";
        String cleaned = s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (cleaned.length() > 80) cleaned = cleaned.substring(0, 80);
        if (cleaned.isEmpty()) cleaned = "MindMap";
        return cleaned;
    }

    // ------------------------------------------------------------- internals

    private static Window activeWindow() {
        for (Window w : Window.getWindows()) {
            if (w.isFocused()) return w;
        }
        for (Window w : Window.getWindows()) {
            if (w.isShowing()) return w;
        }
        return null;
    }
}