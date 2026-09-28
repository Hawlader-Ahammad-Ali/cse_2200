package com.mindmap.view.knowledge;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.KnowledgeType;
import com.mindmap.service.KnowledgeService;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.SceneNavigator;
import com.mindmap.util.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Knowledge list + filters. Includes a "Has Takeaway" filter (Phase 10).
 * Clicking a row opens {@link KnowledgeDetailView} in place.
 */
public class KnowledgeView extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeView.class);

    private final KnowledgeService service = ServiceRegistry.knowledgeService();

    private final ObservableList<KnowledgeItem> data = FXCollections.observableArrayList();

    private final TableView<KnowledgeItem> table = new TableView<>(data);
    private final TextField        searchField    = new TextField();
    private final ComboBox<String> typeFilter     = new ComboBox<>();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final CheckBox         takeawayFilter = new CheckBox("Has Takeaway");
    private final Label            countLabel     = new Label("0 items");

    private final VBox      listRoot;
    private final StackPane contentStack = new StackPane();

    // ------------------------------------------------------ static deep-link handoff
    private static KnowledgeView currentInstance;
    private static Integer pendingKnowledgeId;

    public KnowledgeView() {
        currentInstance = this;

        listRoot = buildListRoot();
        contentStack.getChildren().add(listRoot);
        getChildren().add(contentStack);
        refresh();

        if (pendingKnowledgeId != null) {
            final int id = pendingKnowledgeId;
            pendingKnowledgeId = null;
            Platform.runLater(() -> openKnowledgeById(id));
        }
    }

    // ------------------------------------------------------------- list

    private VBox buildListRoot() {
        Label title = new Label("Knowledge");
        title.getStyleClass().add("view-header");

        countLabel.getStyleClass().add("view-subtitle");
        countLabel.setPadding(new Insets(4, 0, 0, 0));

        VBox titleBox = new VBox(0, title, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+  Add Knowledge");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openAddDialog());

        HBox header = new HBox(10, titleBox, spacer, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));

        searchField.getStyleClass().add("source-search");
        searchField.setPromptText("🔍  Search by title, description, takeaway, category…");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((o, ov, nv) -> refresh());

        typeFilter.getItems().add("All Types");
        for (KnowledgeType t : KnowledgeType.values()) typeFilter.getItems().add(t.name());
        typeFilter.setValue("All Types");
        typeFilter.getStyleClass().add("source-filter");
        typeFilter.setOnAction(e -> refresh());

        categoryFilter.getItems().add("All Categories");
        categoryFilter.setValue("All Categories");
        categoryFilter.getStyleClass().add("source-filter");
        categoryFilter.setOnAction(e -> refresh());

        takeawayFilter.getStyleClass().add("takeaway-filter-checkbox");
        takeawayFilter.selectedProperty().addListener((o, ov, nv) -> refresh());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            typeFilter.setValue("All Types");
            categoryFilter.setValue("All Categories");
            takeawayFilter.setSelected(false);
        });

        HBox filters = new HBox(10, searchField, typeFilter, categoryFilter,
                takeawayFilter, clearBtn);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(4, 0, 8, 0));

        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox root = new VBox(6, header, filters, table);
        root.setPadding(new Insets(4, 0, 0, 0));
        return root;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No knowledge items yet — click \"Add Knowledge\" to begin."));

        TableColumn<KnowledgeItem, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        titleCol.setPrefWidth(260);

        TableColumn<KnowledgeItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getItemType().getIcon() + " " + cd.getValue().getItemType().getLabel()));
        typeCol.setPrefWidth(110);

        TableColumn<KnowledgeItem, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCategory() == null ? "" : cd.getValue().getCategory()));
        catCol.setPrefWidth(130);

        TableColumn<KnowledgeItem, String> impCol = new TableColumn<>("Importance");
        impCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getImportance()));
        impCol.setPrefWidth(90);

        TableColumn<KnowledgeItem, String> takeawayCol = new TableColumn<>("Takeaway");
        takeawayCol.setCellValueFactory(cd -> {
            String note = cd.getValue().getPersonalNote();
            if (note == null || note.isBlank()) return new SimpleStringProperty("");
            String snippet = note.length() > 45 ? note.substring(0, 42) + "…" : note;
            return new SimpleStringProperty("📝  " + snippet);
        });
        takeawayCol.setPrefWidth(240);

        TableColumn<KnowledgeItem, String> originCol = new TableColumn<>("Origin");
        originCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrigin().getIcon() + " " + cd.getValue().getOrigin().getLabel()));
        originCol.setPrefWidth(130);

        table.getColumns().addAll(titleCol, typeCol, catCol, impCol, takeawayCol, originCol);

        table.setRowFactory(tv -> {
            TableRow<KnowledgeItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) showDetail(row.getItem());
            });
            return row;
        });
    }

    // ------------------------------------------------------------- refresh

    private void refresh() {
        try {
            List<String> cats = service.getCategories();
            String currentCat = categoryFilter.getValue();
            categoryFilter.getItems().setAll("All Categories");
            categoryFilter.getItems().addAll(cats);
            if (currentCat != null && categoryFilter.getItems().contains(currentCat)) {
                categoryFilter.setValue(currentCat);
            } else {
                categoryFilter.setValue("All Categories");
            }

            List<KnowledgeItem> rows = service.getAll();

            String typeSel = typeFilter.getValue();
            if (typeSel != null && !typeSel.equals("All Types")) {
                KnowledgeType t = KnowledgeType.valueOf(typeSel);
                rows = rows.stream().filter(k -> k.getItemType() == t).toList();
            }

            String catSel = categoryFilter.getValue();
            if (catSel != null && !catSel.equals("All Categories")) {
                rows = rows.stream().filter(k -> catSel.equals(k.getCategory())).toList();
            }

            if (takeawayFilter.isSelected()) {
                rows = rows.stream()
                        .filter(k -> k.getPersonalNote() != null && !k.getPersonalNote().isBlank())
                        .toList();
            }

            String q = searchField.getText();
            if (q != null && !q.isBlank()) {
                String lower = q.toLowerCase();
                rows = rows.stream().filter(k ->
                        contains(k.getTitle(), lower)
                                || contains(k.getDescription(), lower)
                                || contains(k.getPersonalNote(), lower)
                                || contains(k.getCategory(), lower)
                ).toList();
            }

            data.setAll(rows);
            countLabel.setText(data.size() + (data.size() == 1 ? " item" : " items"));

        } catch (ServiceException e) {
            Dialogs.error("Could not load knowledge", e.getMessage());
        }
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    // ------------------------------------------------------------- dialogs

    private void openAddDialog() {
        KnowledgeFormDialog dlg = new KnowledgeFormDialog(null);
        Optional<KnowledgeItem> result = dlg.showAndWait();
        result.ifPresent(k -> {
            try {
                service.create(k, dlg.getTagNames(), dlg.getSelectedSourceIds());
                AppContext.getInstance().setStatusMessage("Knowledge created: " + k.getTitle());
                refresh();
            } catch (ServiceException e) {
                Dialogs.error("Could not save item", e.getMessage());
            }
        });
    }

    private void openEditDialog(KnowledgeItem item) {
        KnowledgeFormDialog dlg = new KnowledgeFormDialog(item);
        Optional<KnowledgeItem> result = dlg.showAndWait();
        result.ifPresent(k -> {
            try {
                service.update(k, dlg.getTagNames(), dlg.getSelectedSourceIds());
                AppContext.getInstance().setStatusMessage("Knowledge updated: " + k.getTitle());
                showDetail(k);
            } catch (ServiceException e) {
                Dialogs.error("Could not update item", e.getMessage());
            }
        });
    }

    private void confirmDelete(KnowledgeItem item) {
        boolean ok = Dialogs.confirm(
                "Delete this knowledge item?",
                "\"" + item.getTitle() + "\" will be removed. "
                        + "Any graph connections to it will also be deleted.");
        if (!ok) return;

        try {
            service.delete(item.getId());
            AppContext.getInstance().setStatusMessage("Deleted: " + item.getTitle());
            showList();
        } catch (ServiceException e) {
            Dialogs.error("Could not delete item", e.getMessage());
        }
    }

    // ------------------------------------------------------------- navigation

    private void showDetail(KnowledgeItem item) {
        KnowledgeDetailView detail = new KnowledgeDetailView(
                item,
                this::openEditDialog,
                () -> confirmDelete(item),
                this::showList
        );
        contentStack.getChildren().setAll(detail);
    }

    private void showList() {
        contentStack.getChildren().setAll(listRoot);
        refresh();
    }

    // ------------------------------------------------------ deep linking

    public static void navigateAndOpen(int knowledgeId) {
        SceneNavigator nav = SceneNavigator.getInstance();
        if (SceneNavigator.KNOWLEDGE.equals(nav.getCurrentViewId()) && currentInstance != null) {
            currentInstance.openKnowledgeById(knowledgeId);
        } else {
            pendingKnowledgeId = knowledgeId;
            nav.navigateTo(SceneNavigator.KNOWLEDGE);
        }
    }

    private void openKnowledgeById(int id) {
        try {
            service.getById(id).ifPresent(this::showDetail);
        } catch (ServiceException e) {
            Dialogs.error("Could not open item", e.getMessage());
        }
    }
}