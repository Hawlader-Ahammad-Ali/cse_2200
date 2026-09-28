package com.mindmap;

import com.mindmap.config.AppConfig;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.database.DatabaseManager;
import com.mindmap.database.SchemaInitializer;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.AppPaths;
import com.mindmap.util.AppPreferences;
import com.mindmap.util.RootNavigator;
import com.mindmap.util.SessionManager;
import com.mindmap.util.exceptions.AuthException;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MindMap — Personal Knowledge & Learning Discovery Platform.
 *
 * <p>Phase 4 flow:
 * <ol>
 *   <li>init(): directories + DB + schema</li>
 *   <li>start(): restore session if valid, else show Login</li>
 *   <li>Login → MainLayout · Logout → Login</li>
 * </ol>
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Exception startupError;

    public static void main(String[] args) {
        launch(args);
    }

    // ------------------------------------------------------------ JavaFX

    @Override
    public void init() {
        try {
            AppPaths.ensureDirectories();
            DatabaseManager.getInstance().getConnection();
            SchemaInitializer.initialize();
            AppConfig.logStartupSummary();
            log.info("Startup complete.");
        } catch (Exception e) {
            this.startupError = e;
            log.error("Startup failed", e);
        }
    }

    @Override
    public void start(Stage stage) {
        if (startupError != null) {
            showStartupError(stage, startupError);
            return;
        }

        
        RootNavigator.setStage(stage);

        // Restore window state
        stage.setWidth(AppPreferences.getWindowWidth());
        stage.setHeight(AppPreferences.getWindowHeight());
        if (AppPreferences.getWindowX() != -1) stage.setX(AppPreferences.getWindowX());
        if (AppPreferences.getWindowY() != -1) stage.setY(AppPreferences.getWindowY());

        // Save window state on exit
        stage.setOnCloseRequest(e -> {
            AppPreferences.setWindowWidth(stage.getWidth());
            AppPreferences.setWindowHeight(stage.getHeight());
            AppPreferences.setWindowX(stage.getX());
            AppPreferences.setWindowY(stage.getY());
        });


        // -------------------------------------------------- try session restore
        Integer savedUserId = SessionManager.loadUserId();
        if (savedUserId != null) {
            try {
                User user = ServiceRegistry.authService().restoreSession(savedUserId);
                if (user != null) {
                    AppContext.getInstance().setCurrentUser(user);
                    log.info("Auto-login: {}", user.getUsername());
                    RootNavigator.showMainShell();
                    return;
                }
            } catch (AuthException e) {
                log.warn("Session restore failed: {}", e.getMessage());
            }
            SessionManager.clearSession();
        }

        // -------------------------------------------------- show login
        RootNavigator.showLogin();
        log.info("Login screen displayed.");
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
        log.info("MindMap shut down.");
    }

    // ------------------------------------------------------------ error screen

    private void showStartupError(Stage stage, Exception error) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("⚠  MindMap could not start");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #B91C1C;");

        Label message = new Label(buildErrorMessage(error));
        message.setWrapText(true);

        Label detail = new Label(stackTraceHead(error));
        detail.setWrapText(true);
        detail.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px;");

        Region spacer = new Region();
        root.getChildren().addAll(title, message, new Separator(), detail, spacer);

        Scene scene = new Scene(root, 720, 420);
        var cssUrl = getClass().getResource("/css/app.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        stage.setTitle("MindMap — Startup Error");
        stage.setScene(scene);
        stage.show();
    }

    private static String buildErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
        return "Details: " + msg
                + "\n\nCheck that you have write permission to the app data directory,"
                + " and that the application is not already running in another window.";
    }

    private static String stackTraceHead(Exception e) {
        StackTraceElement[] trace = e.getStackTrace();
        StringBuilder sb = new StringBuilder(e.getClass().getName());
        if (e.getMessage() != null) sb.append(": ").append(e.getMessage());
        int limit = Math.min(5, trace.length);
        for (int i = 0; i < limit; i++) sb.append("\n    at ").append(trace[i]);
        if (trace.length > limit) sb.append("\n    ... ").append(trace.length - limit).append(" more");
        return sb.toString();
    }
}