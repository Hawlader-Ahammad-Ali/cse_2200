package com.mindmap.view.settings;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.service.BackupService;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.FileDialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backup & Restore section for the Settings view.
 * Runs export/import on background threads with disabled-button progress state.
 */
public class BackupPanel extends VBox {

    private static final Logger log = LoggerFactory.getLogger(BackupPanel.class);
    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final BackupService service = ServiceRegistry.backupService();

    private final Button exportBtn = new Button("💾  Export Backup");
    private final Button importBtn = new Button("📥  Import Backup");
    private final Label  statusLabel = new Label();

    public BackupPanel() {
        setSpacing(8);

        Label header = new Label("💾  Backup & Restore");
        header.getStyleClass().add("section-header");

        Label desc = new Label("Save all your MindMap data to a single ZIP file, or restore "
                + "from a previous backup. Your data stays on this computer — nothing is uploaded.");
        desc.setWrapText(true);
        desc.getStyleClass().add("placeholder-desc");

        exportBtn.getStyleClass().add("primary-button");
        exportBtn.setOnAction(e -> doExport());

        importBtn.getStyleClass().add("secondary-button");
        importBtn.setOnAction(e -> doImport());

        statusLabel.getStyleClass().add("view-subtitle");
        statusLabel.setStyle("-fx-font-size: 11.5px; -fx-padding: 4 0 0 2;");

        HBox row = new HBox(10, exportBtn, importBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                header,
                desc,
                new Separator(),
                row,
                statusLabel,
                spacer
        );
        setPadding(new Insets(4, 0, 4, 0));
    }

    // =============================================================
    // EXPORT
    // =============================================================

    private void doExport() {
        File dest = FileDialogs.chooseSaveFile(
                "Save Backup",
                BackupService.defaultBackupFileName(),
                "zip");
        if (dest == null) return;

        setButtonsEnabled(false);
        statusLabel.setText("Exporting…");

        Thread worker = new Thread(() -> {
            try {
                BackupService.BackupInfo info =
                        service.exportBackup(dest.toPath());

                Platform.runLater(() -> {
                    setButtonsEnabled(true);
                    statusLabel.setText("Last backup: " + info.createdAt().format(HUMAN));
                    AppContext.getInstance().setStatusMessage(
                            "Backup saved: " + dest.getName());

                    long total = info.tableCounts().values().stream()
                            .mapToInt(Integer::intValue).sum();

                    Dialogs.info("Backup complete",
                            "Saved to:\n" + dest.getAbsolutePath()
                                    + "\n\n" + total + " record" + (total == 1 ? "" : "s")
                                    + " across " + info.tableCounts().size() + " tables.");
                });
            } catch (Throwable t) {
                log.error("Backup export failed", t);
                Platform.runLater(() -> {
                    setButtonsEnabled(true);
                    statusLabel.setText("Export failed.");
                    Dialogs.error("Export failed", safeMsg(t));
                });
            }
        }, "backup-export-worker");
        worker.setDaemon(true);
        worker.start();
    }

    // =============================================================
    // IMPORT
    // =============================================================

    private void doImport() {
        File src = FileDialogs.chooseOpenFile("Open Backup", "zip");
        if (src == null) return;

        // Inspect first — show preview
        BackupService.BackupInfo info;
        try {
            info = service.inspectBackup(src.toPath());
        } catch (ServiceException e) {
            Dialogs.error("Invalid backup file", e.getMessage());
            return;
        }

        long total = info.tableCounts().values().stream()
                .mapToInt(Integer::intValue).sum();

        // Confirm destructive action
        StringBuilder preview = new StringBuilder();
        preview.append("Backup created: ").append(info.createdAt().format(HUMAN)).append('\n');
        preview.append("By user: ").append(info.username()).append('\n');
        preview.append("App version: ").append(info.appVersion()).append('\n');
        preview.append("Records: ").append(total).append(" across ")
                .append(info.tableCounts().size()).append(" tables\n\n");

        preview.append("⚠  WARNING — this will REPLACE all of your current data.\n\n");
        preview.append("Records that will be restored:\n");
        for (Map.Entry<String, Integer> e : info.tableCounts().entrySet()) {
            if (e.getValue() > 0) {
                preview.append("  • ").append(e.getKey())
                        .append(": ").append(e.getValue()).append('\n');
            }
        }

        boolean ok = Dialogs.confirm("Replace your data?", preview.toString());
        if (!ok) {
            statusLabel.setText("Import cancelled.");
            return;
        }

        // Second confirmation — extra guard against accidental destruction
        boolean reallyOk = Dialogs.confirm("Are you absolutely sure?",
                "This cannot be undone. Your current sources, knowledge, reviews, "
                        + "and all other data will be replaced with the backup contents.");
        if (!reallyOk) {
            statusLabel.setText("Import cancelled.");
            return;
        }

        setButtonsEnabled(false);
        statusLabel.setText("Importing…");

        Thread worker = new Thread(() -> {
            try {
                BackupService.ImportResult result =
                        service.importBackup(src.toPath());

                Platform.runLater(() -> {
                    setButtonsEnabled(true);
                    int n = result.totalImported();
                    statusLabel.setText("Last import: " + n + " records restored");
                    AppContext.getInstance().setStatusMessage(
                            "Backup restored: " + n + " record" + (n == 1 ? "" : "s"));

                    Dialogs.info("Import complete",
                            n + " record" + (n == 1 ? "" : "s") + " restored.\n\n"
                                    + "The app will now reload the current view.");
                });
            } catch (Throwable t) {
                log.error("Backup import failed", t);
                Platform.runLater(() -> {
                    setButtonsEnabled(true);
                    statusLabel.setText("Import failed.");
                    Dialogs.error("Import failed", safeMsg(t));
                });
            }
        }, "backup-import-worker");
        worker.setDaemon(true);
        worker.start();
    }

    // =============================================================
    // Helpers
    // =============================================================

    private void setButtonsEnabled(boolean on) {
        exportBtn.setDisable(!on);
        importBtn.setDisable(!on);
    }

    private static String safeMsg(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }

    @SuppressWarnings("unused")
    private final AtomicBoolean unused = new AtomicBoolean(false);
}
