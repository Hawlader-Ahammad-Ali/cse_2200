package com.mindmap.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Small helper for standard information / warning / confirmation dialogs.
 * Always uses the app's stylesheet so dialogs match the theme.
 */
public final class Dialogs {

    private Dialogs() { }

    public static void info(String header, String content) {
        show(Alert.AlertType.INFORMATION, "Information", header, content);
    }

    public static void warning(String header, String content) {
        show(Alert.AlertType.WARNING, "Warning", header, content);
    }

    public static void error(String header, String content) {
        show(Alert.AlertType.ERROR, "Error", header, content);
    }

    /** @return true if the user clicked OK. */
    public static boolean confirm(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyStylesheet(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // --------------------------------------------------------------- internals

    private static void show(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyStylesheet(alert);
        alert.showAndWait();
    }

    private static void applyStylesheet(Alert alert) {
        var cssUrl = Dialogs.class.getResource("/css/app.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());

            String themeFile = AppContext.getInstance().isDarkTheme()
                    ? "/css/theme-dark.css"
                    : "/css/theme-light.css";
            var themeUrl = Dialogs.class.getResource(themeFile);
            if (themeUrl != null) {
                alert.getDialogPane().getStylesheets().add(themeUrl.toExternalForm());
            }
        }
    }
}