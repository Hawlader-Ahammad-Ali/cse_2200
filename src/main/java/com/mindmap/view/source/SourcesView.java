package com.mindmap.view.source;

import com.mindmap.model.KnowledgeItem;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Source;
import com.mindmap.model.SourceStatus;
import com.mindmap.model.SourceType;
import com.mindmap.service.SourceService;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.exceptions.ServiceException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static java.time.zone.ZoneRulesProvider.refresh;

/**
 * Sources list + filter bar. Clicking a row opens {@link SourceDetailView}
 * in place (StackPane swap), so no new window is needed.
 */
public class SourcesView extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(SourcesView.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy");

    private final SourceService service = ServiceRegistry.sourceService();

    private final ObservableList<Source> data = FXCollections.observableArrayList();

    private final TableView<Source> table = new TableView<>(data);
    private final TextField        searchField  = new TextField();
    private final ComboBox<String> typeFilter   = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Label            countLabel   = new Label("0 sources");

    private final VBox listRoot;
    private final StackPane contentStack = new StackPane();

    public SourcesView() {
        listRoot = buildListRoot();

        contentStack.getChildren().add(listRoot);
        getChildren().add(contentStack);

        refresh();
    }

    // ------------------------------------------------------------- list view

    private VBox buildListRoot() {
        // Header row
        Label title = new Label("Sources");
        title.getStyleClass().add("view-header");

        countLabel.getStyleClass().add("view-subtitle");
        countLabel.setPadding(new Insets(4, 0, 0, 0));

        VBox titleBox = new VBox(0, title, countLabel);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button addBtn = new Button("+  Add Source");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openAddDialog());

        HBox header = new HBox(10, titleBox, headerSpacer, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));

        // Filter row
        searchField.getStyleClass().add("source-search");
        searchField.setPromptText("🔍  Search by title, author, description…");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((o, ov, nv) -> refresh());

        typeFilter.getItems().add("All Types");
        for (SourceType t : SourceType.values()) typeFilter.getItems().add(t.name());
        typeFilter.setValue("All Types");
        typeFilter.getStyleClass().add("source-filter");
        typeFilter.setOnAction(e -> refresh());

        statusFilter.getItems().add("All Status");
        for (SourceStatus s : SourceStatus.values()) statusFilter.getItems().add(s.name());
        statusFilter.setValue("All Status");
        statusFilter.getStyleClass().add("source-filter");
        statusFilter.setOnAction(e -> refresh());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            typeFilter.setValue("All Types");
            statusFilter.setValue("All Status");
        });

        HBox filters = new HBox(10, searchField, typeFilter, statusFilter, clearBtn);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(4, 0, 8, 0));

        // Table
        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Assemble
        VBox root = new VBox(6, header, filters, table);
        root.setPadding(new Insets(4, 0, 0, 0));
        return root;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No sources yet — click \"Add Source\" to get started."));

        TableColumn<Source, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        titleCol.setPrefWidth(280);

        TableColumn<Source, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getSourceType().getIcon() + " " +
                        cd.getValue().getSourceType().getLabel()));
        typeCol.setPrefWidth(140);

        TableColumn<Source, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getAuthorCreator() == null ? "" : cd.getValue().getAuthorCreator()));
        authorCol.setPrefWidth(180);

        TableColumn<Source, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStatus().getLabel()));
        statusCol.setPrefWidth(110);

        TableColumn<Source, String> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(cd -> {
            Integer r = cd.getValue().getRating();
            return new SimpleStringProperty(r == null ? "" : "★".repeat(r));
        });
        ratingCol.setPrefWidth(80);

        TableColumn<Source, String> dateCol = new TableColumn<>("Added");
        dateCol.setCellValueFactory(cd -> {
            var d = cd.getValue().getDateAdded();
            return new SimpleStringProperty(d == null ? "" : d.toLocalDate().format(DATE_FMT));
        });
        dateCol.setPrefWidth(110);

        table.getColumns().addAll(titleCol, typeCol, authorCol, statusCol, ratingCol, dateCol);

        // Row click → detail
        table.setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<Source>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() >= 1 && !row.isEmpty()) {
                    showDetail(row.getItem());
                }
            });
            return row;
        });
    }

    // ------------------------------------------------------------- refresh

    private void refresh() {
        try {
            String q = searchField.getText();
            String typeSel = typeFilter.getValue();
            String statusSel = statusFilter.getValue();

            List<Source> rows;
            if ((typeSel == null || typeSel.equals("All Types"))
                    && (statusSel == null || statusSel.equals("All Status"))
                    && (q == null || q.isBlank())) {
                rows = service.getAllSources();
            } else {
                // Start with all, then filter in memory (simple and predictable)
                rows = service.getAllSources();
                if (typeSel != null && !typeSel.equals("All Types")) {
                    SourceType t = SourceType.valueOf(typeSel);
                    rows = rows.stream().filter(s -> s.getSourceType() == t).toList();
                }
                if (statusSel != null && !statusSel.equals("All Status")) {
                    SourceStatus st = SourceStatus.valueOf(statusSel);
                    rows = rows.stream().filter(s -> s.getStatus() == st).toList();
                }
                if (q != null && !q.isBlank()) {
                    String lower = q.toLowerCase();
                    rows = rows.stream().filter(s ->
                            contains(s.getTitle(), lower)
                                    || contains(s.getAuthorCreator(), lower)
                                    || contains(s.getDescription(), lower)
                                    || contains(s.getNotes(), lower)
                    ).toList();
                }
            }

            data.setAll(rows);
            countLabel.setText(data.size() + (data.size() == 1 ? " source" : " sources"));

        } catch (ServiceException e) {
            Dialogs.error("Could not load sources", e.getMessage());
        }
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    // ------------------------------------------------------------- dialogs

    private void openAddDialog() {
        SourceFormDialog dlg = new SourceFormDialog(null);
        Optional<Source> result = dlg.showAndWait();
        result.ifPresent(s -> {
            try {
                service.createSource(s, dlg.getTagNames());
                AppContext.getInstance().setStatusMessage("Source created: " + s.getTitle());
                refresh();
            } catch (ServiceException e) {
                Dialogs.error("Could not save source", e.getMessage());
            }
        });
    }

    private void openEditDialog(Source source) {
        SourceFormDialog dlg = new SourceFormDialog(source);
        Optional<Source> result = dlg.showAndWait();
        result.ifPresent(s -> {
            try {
                service.updateSource(s, dlg.getTagNames());
                AppContext.getInstance().setStatusMessage("Source updated: " + s.getTitle());
                showDetail(s);   // refresh detail view
            } catch (ServiceException e) {
                Dialogs.error("Could not update source", e.getMessage());
            }
        });
    }

    private void confirmDelete(Source source) {
        boolean ok = Dialogs.confirm(
                "Delete this source?",
                "\"" + source.getTitle() + "\" will be permanently removed. "
                        + "Any knowledge items linked to it will remain but lose this source link.");
        if (!ok) return;

        try {
            service.deleteSource(source.getId());
            AppContext.getInstance().setStatusMessage("Deleted: " + source.getTitle());
            showList();
        } catch (ServiceException e) {
            Dialogs.error("Could not delete source", e.getMessage());
        }
    }

    // ------------------------------------------------------------- navigation

    private void showDetail(Source source) {
        SourceDetailView detail = new SourceDetailView(
                source,
                this::openEditDialog,
                () -> confirmDelete(source),
                this::showList,
                this::openKnowledgeItem     // NEW — click a related knowledge row
        );
        contentStack.getChildren().setAll(detail);
    }

    private void showList() {
        contentStack.getChildren().setAll(listRoot);
        refresh();
    }
/**
 * Opens a knowledge item from the source detail view.
 * Since KnowledgeView is a separate top-level view, we navigate to it
 * and dispatch a "select" request via a static hand-off.
 * For Phase 6 we simply navigate to Knowledge; Phase 6+ can add deep linking.
 */
private void openKnowledgeItem(KnowledgeItem item) {
    // Navigate to Knowledge view — the item is visible in the list.
    com.mindmap.util.SceneNavigator.getInstance()
            .navigateTo(com.mindmap.util.SceneNavigator.KNOWLEDGE);
}
}