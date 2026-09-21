package com.mindmap;

import com.mindmap.config.AppConfig;
import com.mindmap.database.DatabaseManager;
import com.mindmap.database.SchemaInitializer;
import com.mindmap.util.AppPaths;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MindMap — Personal Knowledge & Learning Discovery Platform.
 *
 * <p>Phase 1: bootstrap and verify environment.
 * Phase 2 replaces {@link #showBootstrapWindow(Stage)} with the real UI shell.</p>
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Exception startupError;

    public static void main(String[] args) {
        launch(args);
    }

    // ------------------------------------------------------------------ JavaFX

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
        showBootstrapWindow(stage);
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
        log.info("MindMap shut down.");
    }

    // ------------------------------------------------------------------ screens

    /**
     * Phase-1 window. Confirms the toolchain and database are ready.
     * Phase 2 will replace this with the main layout (sidebar + content area).
     */
    private void showBootstrapWindow(Stage stage) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(28, 32, 24, 32));
        root.setAlignment(Pos.TOP_LEFT);
        root.getStyleClass().add("bootstrap-root");

        Label title = new Label("🧠  MindMap");
        title.getStyleClass().add("bootstrap-title");

        Label subtitle = new Label("Personal Knowledge & Learning Discovery Platform");
        subtitle.getStyleClass().add("bootstrap-subtitle");

        Label header = new Label("Phase 1 — Project Setup");
        header.getStyleClass().add("section-header");

        VBox info = new VBox(6);
        info.getChildren().addAll(
                infoRow("Java",            System.getProperty("java.version")),
                infoRow("JavaFX",          System.getProperty("javafx.runtime.version", "n/a")),
                infoRow("Operating system", System.getProperty("os.name")
                        + " " + System.getProperty("os.version")),
                infoRow("App version",     AppConfig.APP_VERSION),
                infoRow("Data directory",  AppPaths.getAppDataDir().toString()),
                infoRow("Database file",   AppPaths.getDatabaseFile().toString()),
                infoRow("Schema",          "✅ Initialized (v1)"),
                infoRow("AI features",     AppConfig.isAIEnabled()
                        ? "✅ Enabled" : "⚠ Disabled (set MINDMAP_AI_KEY to enable)")
        );

        Label next = new Label(
                "Next: Phase 2 — JavaFX UI skeleton (sidebar navigation, themes, views).");
        next.getStyleClass().add("next-hint");

        root.getChildren().addAll(
                title, subtitle,
                new Separator(),
                header, info,
                new Separator(),
                next
        );

        Scene scene = new Scene(root, 720, 460);
        var cssUrl = getClass().getResource("/css/app.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("MindMap");
        stage.setScene(scene);
        stage.setMinWidth(560);
        stage.setMinHeight(400);
        stage.show();

        log.info("Bootstrap window displayed.");
    }

    /** Renders a startup failure without crashing the JVM. */
    private void showStartupError(Stage stage, Exception error) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("bootstrap-root");

        Label title = new Label("⚠  MindMap could not start");
        title.getStyleClass().add("error-title");

        Label message = new Label(buildErrorMessage(error));
        message.setWrapText(true);
        message.getStyleClass().add("info-label");

        Label detail = new Label(stackTraceHead(error));
        detail.setWrapText(true);
        detail.getStyleClass().add("error-detail");

        root.getChildren().addAll(title, message, new Separator(), detail);

        Scene scene = new Scene(root, 720, 420);
        var cssUrl = getClass().getResource("/css/app.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("MindMap — Startup Error");
        stage.setScene(scene);
        stage.show();
    }

    // ------------------------------------------------------------------ utilities

    private static HBox infoRow(String key, String value) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("info-key");
        k.setMinWidth(140);

        Label v = new Label(value);
        v.getStyleClass().add("info-label");
        v.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, k, v, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String buildErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        return "Details: " + msg
                + "\n\nCheck that you have write permission to the app data directory,"
                + " and that the application is not already running in another window.";
    }

    private static String stackTraceHead(Exception e) {
        StackTraceElement[] trace = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName());
        if (e.getMessage() != null) {
            sb.append(": ").append(e.getMessage());
        }
        int limit = Math.min(5, trace.length);
        for (int i = 0; i < limit; i++) {
            sb.append("\n    at ").append(trace[i]);
        }
        if (trace.length > limit) {
            sb.append("\n    ... ").append(trace.length - limit).append(" more");
        }
        return sb.toString();
    }
}