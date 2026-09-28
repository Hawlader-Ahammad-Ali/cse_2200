package com.mindmap;

import com.mindmap.config.AppConfig;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.config.UserPreferences;
import com.mindmap.database.DatabaseManager;
import com.mindmap.database.SchemaInitializer;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.AppPaths;
import com.mindmap.util.SceneNavigator;
import com.mindmap.util.SessionManager;
import com.mindmap.util.WindowStateManager;
import com.mindmap.util.exceptions.AuthException;
import com.mindmap.view.fxml.MainFxmlLauncher;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alternative entry point using FXML for the shell + auth screens.
 * The inner content views (Sources, Knowledge, Graph, etc.) still use the
 * programmatic Views from earlier phases.
 *
 * Run: right-click → Run 'MainFxml.main()'.
 * Or set as main class: `<mainClass>com.mindmap.MainFxml</mainClass>` in pom.xml.
 */
public class MainFxml extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainFxml.class);
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    // ------------------------------------------------------------- life cycle

    @Override
    public void init() {
        try {
            AppPaths.ensureDirectories();
            DatabaseManager.getInstance().getConnection();
            SchemaInitializer.initialize();
            AppConfig.logStartupSummary();

            UserPreferences prefs = UserPreferences.load();
            WindowStateManager.setPreferences(prefs);
            AppContext.getInstance().setTheme(prefs.getTheme());
        } catch (Exception e) {
            log.error("Startup failed", e);
        }
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        MainFxmlLauncher.setStage(stage);

        // Try session restore first
        Integer savedUserId = SessionManager.loadUserId();
        if (savedUserId != null) {
            try {
                User user = ServiceRegistry.authService().restoreSession(savedUserId);
                if (user != null) {
                    AppContext.getInstance().setCurrentUser(user);
                    MainFxmlLauncher.showMainShell();
                    return;
                }
            } catch (AuthException e) {
                log.warn("Session restore failed: {}", e.getMessage());
            }
            SessionManager.clearSession();
        }

        MainFxmlLauncher.showLogin();
    }

    @Override
    public void stop() {
        try {
            WindowStateManager.save(primaryStage);
        } catch (Exception ignore) { }
        DatabaseManager.getInstance().close();
        log.info("MindMap (FXML) shut down.");
    }
}