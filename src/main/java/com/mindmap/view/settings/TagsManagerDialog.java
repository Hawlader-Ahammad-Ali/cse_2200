package com.mindmap.view.settings;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.dao.TagDAO;
import com.mindmap.dao.impl.TagDAOImpl;
import com.mindmap.model.Tag;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Modal dialog for managing the current user's tags:
 * rename, change color, delete. New tags are auto-created when the
 * user types them into source/knowledge forms — so this dialog is for
 * cleanup and recoloring.
 */
public class TagsManagerDialog {

    private static final Logger log = LoggerFactory.getLogger(TagsManagerDialog.class);

    private final TagDAO tagDAO = new TagDAOImpl();
    private final Dialog<Void> dialog = new Dialog<>();
    private final ObservableList<Tag> data = FXCollections.observableArrayList();
    private final ListView<Tag> list = new ListView<>(data);

    public TagsManagerDialog() {
        dialog.setTitle("Manage Tags");
        dialog.setResizable(true);
        buildUI();
        loadTags();
    }

    public void showAndWait() { dialog.showAndWait(); }

    // ------------------------------------------------------------- UI

    private void buildUI() {
        var pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        applyStylesheet(pane);

        Label header = new Label("Your Tags");
        header.getStyleClass().add("section-header");

        Label hint = new Label("Rename, recolor, or delete tags. Deleting a tag removes it "
                + "from all sources and knowledge items but does not delete those items.");
        hint.setWrapText(true);
        hint.getStyleClass().add("placeholder-desc");

        // Custom cell: colored dot + name
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Tag t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Circle dot = new Circle(6);
                try {
                    dot.setFill(Color.web(t.getColor() == null ? "#7E57C2" : t.getColor()));
                } catch (Exception ex) {
                    dot.setFill(Color.web("#7E57C2"));
                }
                Label name = new Label(t.getName());
                name.setStyle("-fx-font-size: 13px;");
                HBox row = new HBox(10, dot, name);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });
        list.setPrefHeight(280);
        VBox.setVgrow(list, Priority.ALWAYS);

        // Actions
        Button renameBtn = new Button("✎  Rename");
        renameBtn.getStyleClass().add("secondary-button");
        renameBtn.setOnAction(e -> renameSelected());

        Button colorBtn = new Button("🎨  Change Color");
        colorBtn.getStyleClass().add("secondary-button");
        colorBtn.setOnAction(e -> changeColorSelected());

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> deleteSelected());

        HBox actions = new HBox(8, renameBtn, colorBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(6, 0, 0, 0));

        VBox root = new VBox(10, header, hint, list, actions);
        root.setPadding(new Insets(10, 14, 8, 14));
        root.setPrefWidth(460);
        root.setPrefHeight(420);

        pane.setContent(root);
    }

    private void applyStylesheet(javafx.scene.control.DialogPane pane) {
        var css = getClass().getResource("/css/app.css");
        if (css != null) pane.getStylesheets().add(css.toExternalForm());

        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var themeCss = getClass().getResource(theme);
        if (themeCss != null) pane.getStylesheets().add(themeCss.toExternalForm());
    }

    // ------------------------------------------------------------- data

    private void loadTags() {
        try {
            int uid = currentUserId();
            data.setAll(tagDAO.findByUser(uid));
        } catch (SQLException e) {
            log.error("Could not load tags", e);
            Dialogs.error("Could not load tags", "Please try again later.");
        }
    }

    // ------------------------------------------------------------- actions

    private void renameSelected() {
        Tag t = list.getSelectionModel().getSelectedItem();
        if (t == null) { Dialogs.warning("No tag selected", "Pick a tag first."); return; }

        TextField input = new TextField(t.getName());
        Optional<String> newName = promptForText("Rename Tag", "New name:", input);
        if (newName.isEmpty()) return;

        String trimmed = newName.get().trim();
        if (trimmed.isEmpty()) {
            Dialogs.warning("Invalid name", "Tag name must not be empty.");
            return;
        }
        if (trimmed.equalsIgnoreCase(t.getName())) return;

        try {
            int uid = currentUserId();
            if (tagDAO.findByName(uid, trimmed).isPresent()) {
                Dialogs.warning("Name in use", "A tag with that name already exists.");
                return;
            }
            Tag updated = new Tag();
            updated.setId(t.getId());
            updated.setUserId(t.getUserId());
            updated.setName(trimmed);
            updated.setColor(t.getColor());
            updateTag(updated);
            loadTags();
        } catch (SQLException e) {
            Dialogs.error("Could not rename", e.getMessage());
        }
    }

    private void changeColorSelected() {
        Tag t = list.getSelectionModel().getSelectedItem();
        if (t == null) { Dialogs.warning("No tag selected", "Pick a tag first."); return; }

        // Simple 8-color palette choice
        String[] palette = {
                "#7E57C2", "#1ABC9C", "#3498DB", "#27AE60",
                "#F39C12", "#E74C3C", "#9B59B6", "#34495E"
        };

        javafx.scene.control.ChoiceDialog<String> choice =
                new javafx.scene.control.ChoiceDialog<>(t.getColor(),
                        java.util.List.of(palette));
        choice.setTitle("Tag Color");
        choice.setHeaderText("Choose a new color for \"" + t.getName() + "\"");
        Optional<String> selected = choice.showAndWait();
        if (selected.isEmpty()) return;

        try {
            Tag updated = new Tag();
            updated.setId(t.getId());
            updated.setUserId(t.getUserId());
            updated.setName(t.getName());
            updated.setColor(selected.get());
            updateTag(updated);
            loadTags();
        } catch (SQLException e) {
            Dialogs.error("Could not change color", e.getMessage());
        }
    }

    private void deleteSelected() {
        Tag t = list.getSelectionModel().getSelectedItem();
        if (t == null) { Dialogs.warning("No tag selected", "Pick a tag first."); return; }

        boolean ok = Dialogs.confirm("Delete tag?",
                "\"" + t.getName() + "\" will be removed from all sources and knowledge items. "
                        + "The items themselves are not deleted.");
        if (!ok) return;

        try {
            tagDAO.delete(t.getId());
            loadTags();
        } catch (SQLException e) {
            Dialogs.error("Could not delete tag", e.getMessage());
        }
    }

    // ------------------------------------------------------------- helpers

    /** The TagDAO interface has no update method — we run a small inline update. */
    private void updateTag(Tag t) throws SQLException {
        var conn = com.mindmap.database.DatabaseManager.getInstance().getConnection();
        try (var ps = conn.prepareStatement(
                "UPDATE tags SET name = ?, color = ? WHERE id = ?")) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getColor());
            ps.setInt(3, t.getId());
            ps.executeUpdate();
        }
    }

    private int currentUserId() {
        User u = AppContext.getInstance().getCurrentUser();
        return u == null ? -1 : u.getId();
    }

    private Optional<String> promptForText(String title, String prompt, TextField input) {
        javafx.scene.control.TextInputDialog dlg =
                new javafx.scene.control.TextInputDialog(input.getText());
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.setContentText(prompt);
        applyStylesheet(dlg.getDialogPane());
        return dlg.showAndWait();
    }
}