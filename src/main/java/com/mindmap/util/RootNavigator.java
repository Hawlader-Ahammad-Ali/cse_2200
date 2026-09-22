package com.mindmap.util;

import com.mindmap.view.MainLayout;
import com.mindmap.view.auth.LoginView;
import com.mindmap.view.auth.RegisterView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root-level scene switcher. Distinct from {@link SceneNavigator}:
 * <ul>
 *   <li>{@code RootNavigator}  → swaps the whole window (auth ↔ main shell)</li>
 *   <li>{@code SceneNavigator} → swaps the content inside MainLayout</li>
 * </ul>
 */
public final class RootNavigator {

    private static final Logger log = LoggerFactory.getLogger(RootNavigator.class);

    private static final int AUTH_WIDTH    = 1000;
    private static final int AUTH_HEIGHT   = 680;
    private static final int AUTH_MIN_W    = 780;
    private static final int AUTH_MIN_H    = 580;

    private static final int MAIN_WIDTH    = 1280;
    private static final int MAIN_HEIGHT   = 800;
    private static final int MAIN_MIN_W    = 1000;
    private static final int MAIN_MIN_H    = 650;

    private static Stage primaryStage;

    private RootNavigator() { }

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    // ------------------------------------------------------------ screens

    public static void showLogin() {
        swapScene(new LoginView(),
                "MindMap — Sign In",
                AUTH_WIDTH, AUTH_HEIGHT,
                AUTH_MIN_W, AUTH_MIN_H);
    }

    public static void showRegister() {
        swapScene(new RegisterView(),
                "MindMap — Create Account",
                AUTH_WIDTH, AUTH_HEIGHT,
                AUTH_MIN_W, AUTH_MIN_H);
    }

    public static void showMainShell() {
        MainLayout layout = new MainLayout();
        swapScene(layout,
                "MindMap — Personal Knowledge & Learning Discovery Platform",
                MAIN_WIDTH, MAIN_HEIGHT,
                MAIN_MIN_W, MAIN_MIN_H);
        installMainShellShortcuts(layout.getScene(), layout);
    }

    // ------------------------------------------------------------ internals

    private static void swapScene(Parent root,
                                  String title,
                                  int width, int height,
                                  int minWidth, int minHeight) {
        if (primaryStage == null) {
            log.error("Primary stage not set — call RootNavigator.setStage() first.");
            return;
        }

        Scene scene = new Scene(root, width, height);
        applyTheme(scene);

        // React to theme changes at runtime
        AppContext.getInstance().themeProperty().addListener((obs, o, n) -> applyTheme(scene));

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(minWidth);
        primaryStage.setMinHeight(minHeight);
        primaryStage.centerOnScreen();
        primaryStage.show();

        log.debug("Scene swapped: {}", title);
    }

    private static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();

        addStylesheet(scene, "/css/app.css");
        addStylesheet(scene, "/css/auth.css");

        String themeFile = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css"
                : "/css/theme-light.css";
        addStylesheet(scene, themeFile);
    }

    private static void addStylesheet(Scene scene, String path) {
        var url = RootNavigator.class.getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            log.warn("Stylesheet not found on classpath: {}", path);
        }
    }

    // ------------------------------------------------------------ shortcuts (main shell only)

    private static void installMainShellShortcuts(Scene scene, MainLayout layout) {
        SceneNavigator nav = SceneNavigator.getInstance();

        bind(scene, KeyCode.K, KeyCombination.CONTROL_DOWN, layout.getTopBar()::focusSearch);
        bind(scene, KeyCode.N, KeyCombination.CONTROL_DOWN,
                () -> nav.navigateTo(SceneNavigator.SOURCES));
        bind(scene, KeyCode.R, KeyCombination.CONTROL_DOWN,
                () -> nav.navigateTo(SceneNavigator.REVIEW));
        bind(scene, KeyCode.Q, KeyCombination.CONTROL_DOWN,
                () -> nav.navigateTo(SceneNavigator.QUIZ));
        bind(scene, KeyCode.T, KeyCombination.CONTROL_DOWN, () -> {
            AppContext.getInstance().toggleTheme();
            layout.getTopBar().refreshThemeIcon();
        });

        bind(scene, KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.DASHBOARD));
        bind(scene, KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.SOURCES));
        bind(scene, KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.KNOWLEDGE));
        bind(scene, KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.GRAPH));
        bind(scene, KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.REVIEW));
        bind(scene, KeyCode.DIGIT6, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.FLASHCARDS));
        bind(scene, KeyCode.DIGIT7, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.QUIZ));
        bind(scene, KeyCode.DIGIT8, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.TIMELINE));
        bind(scene, KeyCode.DIGIT9, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.ANALYTICS));
        bind(scene, KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN, () -> nav.navigateTo(SceneNavigator.SETTINGS));

        // F11 → full screen (no modifier — dedicated constructor, no NO_MODIFIER constant)
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F11),
                () -> {
                    Stage s = (Stage) scene.getWindow();
                    s.setFullScreen(!s.isFullScreen());
                });

        bind(scene, KeyCode.LEFT, KeyCombination.ALT_DOWN, nav::goBack);
    }

    private static void bind(Scene scene,
                             KeyCode code,
                             KeyCombination.Modifier mod,
                             Runnable action) {
        scene.getAccelerators().put(new KeyCodeCombination(code, mod), action);
    }
}