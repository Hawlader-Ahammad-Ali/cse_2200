package com.mindmap.view.fxml;

import com.mindmap.util.AppContext;
import com.mindmap.util.WindowStateManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Small router that swaps FXML scenes. Kept separate from {@code RootNavigator}
 * so both entry points can coexist.
 */
public final class MainFxmlLauncher {

    private static final Logger log = LoggerFactory.getLogger(MainFxmlLauncher.class);

    private static Stage stage;

    private MainFxmlLauncher() { }

    public static void setStage(Stage s) { stage = s; }

    // ------------------------------------------------------------- screens

    public static void showLogin() {
        swapScene("/fxml/login.fxml",
                "MindMap — Sign In",
                1000, 680, 780, 580);
    }

    public static void showRegister() {
        swapScene("/fxml/register.fxml",
                "MindMap — Create Account",
                1000, 680, 780, 580);
    }

    public static void showMainShell() {
        swapScene("/fxml/main_layout.fxml",
                "MindMap — Personal Knowledge & Learning Discovery Platform",
                1280, 800, 1000, 650);
        WindowStateManager.apply(stage);
    }

    // ------------------------------------------------------------- internals

    private static void swapScene(String fxmlPath,
                                  String title,
                                  int width, int height,
                                  int minWidth, int minHeight) {
        if (stage == null) {
            log.error("Stage not set — call MainFxmlLauncher.setStage() first.");
            return;
        }

        try {
            URL url = MainFxmlLauncher.class.getResource(fxmlPath);
            if (url == null) {
                log.error("FXML not found on classpath: {}", fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(url);

            Scene scene = new Scene(root, width, height);
            applyTheme(scene);

            AppContext.getInstance().themeProperty().addListener((obs, o, n) -> applyTheme(scene));

            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);
            stage.centerOnScreen();
            stage.show();

            log.debug("FXML scene loaded: {}", fxmlPath);

        } catch (IOException e) {
            log.error("Could not load FXML: {}", fxmlPath, e);
        }
    }

    private static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        addStylesheet(scene, "/css/app.css");
        addStylesheet(scene, "/css/auth.css");
        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        addStylesheet(scene, theme);
    }

    private static void addStylesheet(Scene scene, String path) {
        var url = MainFxmlLauncher.class.getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            log.warn("Stylesheet not found: {}", path);
        }
    }
}