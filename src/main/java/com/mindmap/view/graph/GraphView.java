package com.mindmap.view.graph;

import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.Connection;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.RelationshipType;
import com.mindmap.model.Source;
import com.mindmap.service.GraphService;
import com.mindmap.util.AppContext;
import com.mindmap.util.Dialogs;
import com.mindmap.util.SceneNavigator;
import com.mindmap.util.exceptions.ServiceException;
import com.mindmap.view.knowledge.KnowledgeView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Knowledge graph view:
 *   ┌─────────────────────────────────────────────────┬──────────────────┐
 *   │ Toolbar (Search · Add connection · Reset layout)│                  │
 *   ├─────────────────────────────────────────────────┤  Side panel      │
 *   │                                                 │  (selected node) │
 *   │               GraphCanvas                       │                  │
 *   │                                                 │                  │
 *   └─────────────────────────────────────────────────┴──────────────────┘
 */
public class GraphView extends BorderPane {

    private static final Logger log = LoggerFactory.getLogger(GraphView.class);

    private final GraphService service = ServiceRegistry.graphService();

    private final GraphCanvas canvas = new GraphCanvas();
    private final TextField   searchField = new TextField();
    private final Label       statsLabel  = new Label();
    private final VBox        sidePanel   = new VBox(12);
    private final ScrollPane  sideScroll  = new ScrollPane(sidePanel);

    private List<KnowledgeItem> nodes;
    private List<Connection>    edges;
    private Map<Integer, Integer> crossSource;

    public GraphView() {
        buildUI();
        loadGraph();
    }

    // ------------------------------------------------------------- UI

    private void buildUI() {
        // Toolbar
        Label title = new Label("Knowledge Graph");
        title.getStyleClass().add("view-header");
        title.setPadding(new Insets(0, 12, 0, 0));

        searchField.getStyleClass().add("source-search");
        searchField.setPromptText("🔍  Highlight nodes…");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((o, ov, nv) -> canvas.searchHighlight(nv));

        Button addBtn = new Button("+  Add Connection");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openConnectionDialog());

        Button rerunBtn = new Button("↻  Re-run layout");
        rerunBtn.getStyleClass().add("secondary-button");
        rerunBtn.setOnAction(e -> canvas.rerunLayout());

        Button resetBtn = new Button("⟲  Reset view");
        resetBtn.getStyleClass().add("secondary-button");
        resetBtn.setOnAction(e -> canvas.resetView());

        statsLabel.getStyleClass().add("view-subtitle");
        statsLabel.setPadding(new Insets(0, 0, 0, 4));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, title, statsLabel, spacer,
                searchField, addBtn, rerunBtn, resetBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        // Center: canvas
        StackPane canvasWrap = new StackPane(canvas);
        canvasWrap.setStyle("-fx-background-color: -fx-bg-primary; "
                + "-fx-background-radius: 12; -fx-border-radius: 12; "
                + "-fx-border-color: -fx-border-color-app;");
        canvasWrap.setMinHeight(400);

        // Right: side panel
        sideScroll.setFitToWidth(true);
        sideScroll.setPrefWidth(320);
        sideScroll.setStyle("-fx-background-color: transparent;");
        sidePanel.setPadding(new Insets(12));
        showEmptySidePanel();

        setCenter(new VBox(toolbar, canvasWrap));
        VBox.setVgrow(canvasWrap, Priority.ALWAYS);
        setRight(sideScroll);

        // Wire canvas callbacks
        canvas.setOnNodeSelected(this::showNodeInSidePanel);
        canvas.setOnNodeDoubleClick(n -> KnowledgeView.navigateAndOpen(n.getId()));
        canvas.setOnNodeRightClick(this::showNodeContextMenu);

        // If the user switches away, pause the simulation
        sceneProperty().addListener((o, ov, nv) -> {
            if (nv == null) canvas.pause();
        });
    }

    // ------------------------------------------------------------- data

    private void loadGraph() {
        try {
            nodes = service.getNodes();
            edges = service.getEdges();
            crossSource = service.findCrossSourceItems();

            canvas.loadGraph(nodes, edges, crossSource);
            updateStats();

            log.info("Loaded graph: {} nodes, {} edges, {} cross-source items",
                    nodes.size(), edges.size(), crossSource.size());

        } catch (ServiceException e) {
            Dialogs.error("Could not load graph", e.getMessage());
            statsLabel.setText("Could not load graph");
        }
    }

    private void updateStats() {
        int n = nodes == null ? 0 : nodes.size();
        int e = edges == null ? 0 : edges.size();
        int x = crossSource == null ? 0 : crossSource.size();
        statsLabel.setText(n + " nodes · " + e + " edges"
                + (x > 0 ? " · " + x + " cross-source" : ""));
    }

    // ------------------------------------------------------------- side panel

    private void showEmptySidePanel() {
        sidePanel.getChildren().clear();

        Label header = new Label("Node details");
        header.getStyleClass().add("section-header");

        Label hint = new Label("Click a node in the graph to see its details, "
                + "connections, and sources. Double-click to open the knowledge item.");
        hint.setWrapText(true);
        hint.getStyleClass().add("placeholder-desc");

        sidePanel.getChildren().addAll(header, hint);
    }

    private void showNodeInSidePanel(GraphNode node) {
        try {
            Set<Integer> neighbors = service.neighborIds(node.getId());
            canvas.highlightNode(node, neighbors);
        } catch (ServiceException e) {
            // non-fatal: just show the node without highlight
        }

        sidePanel.getChildren().clear();

        // Title card
        Label icon = new Label(node.item.getItemType().getIcon());
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label(node.item.getTitle());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");
        title.setWrapText(true);

        HBox titleRow = new HBox(8, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        sidePanel.getChildren().add(titleRow);

        // Meta
        sidePanel.getChildren().add(metaRow("Type", node.item.getItemType().getLabel()));
        if (node.item.getCategory() != null) {
            sidePanel.getChildren().add(metaRow("Category", node.item.getCategory()));
        }
        sidePanel.getChildren().add(metaRow("Confidence",
                "★".repeat(node.item.getConfidence())
                        + "☆".repeat(5 - node.item.getConfidence())));
        sidePanel.getChildren().add(metaRow("Difficulty", node.item.getDifficulty()));
        sidePanel.getChildren().add(metaRow("Importance", node.item.getImportance()));

        // Cross-source badge
        if (crossSource != null && crossSource.containsKey(node.getId())) {
            int count = crossSource.get(node.getId());
            Label badge = new Label("🔀  Explored through " + count + " sources");
            badge.setStyle("-fx-background-color: -fx-accent-soft; -fx-text-fill: -fx-accent-hover; "
                    + "-fx-padding: 4 10 4 10; -fx-background-radius: 12; -fx-font-size: 11.5px; "
                    + "-fx-font-weight: bold;");
            sidePanel.getChildren().add(badge);
        }

        // Description
        if (node.item.getDescription() != null && !node.item.getDescription().isBlank()) {
            sidePanel.getChildren().add(new Separator());
            Label d = new Label(node.item.getDescription());
            d.setWrapText(true);
            d.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12.5px;");
            sidePanel.getChildren().add(d);
        }

        // Takeaway
        if (node.item.getPersonalNote() != null && !node.item.getPersonalNote().isBlank()) {
            sidePanel.getChildren().add(new Separator());
            Label h = new Label("📝  My Takeaway");
            h.getStyleClass().add("section-header");
            Label t = new Label(node.item.getPersonalNote());
            t.setWrapText(true);
            t.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 12.5px;");
            sidePanel.getChildren().addAll(h, t);
        }

        // Sources ("Where did I learn this?")
        try {
            List<Source> sources = service.sourcesFor(node.getId());
            if (!sources.isEmpty()) {
                sidePanel.getChildren().add(new Separator());
                Label h = new Label("📍  Where Did I Learn This?");
                h.getStyleClass().add("section-header");
                sidePanel.getChildren().add(h);
                for (Source s : sources) {
                    HBox r = new HBox(6);
                    Label sIcon = new Label(s.getSourceType().getIcon());
                    Label sTitle = new Label(s.getTitle());
                    sTitle.setWrapText(true);
                    sTitle.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");
                    r.getChildren().addAll(sIcon, sTitle);
                    sidePanel.getChildren().add(r);
                }
            }
        } catch (ServiceException e) {
            // ignore
        }

        // Connections
        try {
            List<Connection> conns = service.connectionsFor(node.getId());
            if (!conns.isEmpty()) {
                sidePanel.getChildren().add(new Separator());
                Label h = new Label("🔗  Connections (" + conns.size() + ")");
                h.getStyleClass().add("section-header");
                sidePanel.getChildren().add(h);
                for (Connection c : conns) {
                    int otherId = (c.getSourceItemId() == node.getId())
                            ? c.getTargetItemId() : c.getSourceItemId();
                    Optional<KnowledgeItem> other = service.getById(otherId);
                    if (other.isEmpty()) continue;

                    HBox r = new HBox(6);
                    r.setAlignment(Pos.CENTER_LEFT);

                    Label arrow = new Label(c.getSourceItemId() == node.getId() ? "→" : "←");
                    arrow.setStyle("-fx-text-fill: -fx-text-muted;");

                    Label oIcon = new Label(other.get().getItemType().getIcon());
                    Label oTitle = new Label(other.get().getTitle());
                    oTitle.setWrapText(true);
                    oTitle.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 12px;");
                    HBox.setHgrow(oTitle, Priority.ALWAYS);

                    Label rel = new Label(c.getRelationshipType().getLabel());
                    rel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 10.5px;");

                    Button removeBtn = new Button("✕");
                    removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-danger; "
                            + "-fx-cursor: hand; -fx-padding: 2 6 2 6;");
                    removeBtn.setOnAction(e -> deleteConnection(c));

                    r.getChildren().addAll(arrow, oIcon, oTitle, rel, removeBtn);
                    sidePanel.getChildren().add(r);
                }
            }
        } catch (ServiceException e) {
            // ignore
        }

        // Actions
        sidePanel.getChildren().add(new Separator());

        Button openBtn = new Button("Open details");
        openBtn.getStyleClass().add("primary-button");
        openBtn.setMaxWidth(Double.MAX_VALUE);
        openBtn.setOnAction(e -> KnowledgeView.navigateAndOpen(node.getId()));

        Button addConnBtn = new Button("+  Add connection from here");
        addConnBtn.getStyleClass().add("secondary-button");
        addConnBtn.setMaxWidth(Double.MAX_VALUE);
        addConnBtn.setOnAction(e -> openConnectionDialog(node));

        sidePanel.getChildren().addAll(openBtn, addConnBtn);
    }

    private HBox metaRow(String key, String value) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11.5px; -fx-min-width: 80;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 12px;");
        HBox r = new HBox(6, k, v);
        r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    // ------------------------------------------------------------- context menu

    private void showNodeContextMenu(GraphNode node) {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open knowledge item");
        openItem.setOnAction(e -> KnowledgeView.navigateAndOpen(node.getId()));

        MenuItem connectItem = new MenuItem("Add connection from here…");
        connectItem.setOnAction(e -> openConnectionDialog(node));

        MenuItem clearItem = new MenuItem("Clear highlight");
        clearItem.setOnAction(e -> {
            canvas.clearHighlight();
            showEmptySidePanel();
        });

        menu.getItems().addAll(openItem, connectItem, new SeparatorMenuItem(), clearItem);
        menu.show(canvas, javafx.geometry.Side.TOP, 0, 0);
        // Position near node would require screen coords; showing in canvas center
        // is acceptable for Phase 8.
    }

    // ------------------------------------------------------------- dialogs

    private void openConnectionDialog() {
        openConnectionDialog(null);
    }

    private void openConnectionDialog(GraphNode fromNode) {
        if (nodes == null || nodes.size() < 2) {
            Dialogs.info("Not enough nodes",
                    "You need at least 2 knowledge items to create a connection.");
            return;
        }

        Dialog<Connection> dlg = new Dialog<>();
        dlg.setTitle("Create Connection");
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        var appCss = getClass().getResource("/css/app.css");
        if (appCss != null) pane.getStylesheets().add(appCss.toExternalForm());
        String theme = AppContext.getInstance().isDarkTheme()
                ? "/css/theme-dark.css" : "/css/theme-light.css";
        var themeCss = getClass().getResource(theme);
        if (themeCss != null) pane.getStylesheets().add(themeCss.toExternalForm());

        ComboBox<KnowledgeItem> sourceBox = new ComboBox<>();
        sourceBox.getItems().addAll(nodes);
        sourceBox.setMaxWidth(Double.MAX_VALUE);
        sourceBox.setButtonCell(itemCell());
        sourceBox.setCellFactory(lv -> itemCell());
        if (fromNode != null) {
            for (KnowledgeItem k : nodes) {
                if (k.getId() == fromNode.getId()) sourceBox.setValue(k);
            }
        } else {
            sourceBox.getSelectionModel().selectFirst();
        }

        ComboBox<KnowledgeItem> targetBox = new ComboBox<>();
        targetBox.getItems().addAll(nodes);
        targetBox.setMaxWidth(Double.MAX_VALUE);
        targetBox.setButtonCell(itemCell());
        targetBox.setCellFactory(lv -> itemCell());

        ComboBox<RelationshipType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(RelationshipType.values());
        typeBox.setValue(RelationshipType.RELATED_TO);
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(2);
        notesArea.setPromptText("Optional notes about this connection");
        notesArea.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 12, 4, 12));

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(90);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        int row = 0;
        grid.add(new Label("From"), 0, row); grid.add(sourceBox, 1, row++);
        grid.add(new Label("To"),   0, row); grid.add(targetBox, 1, row++);
        grid.add(new Label("Relationship"), 0, row); grid.add(typeBox, 1, row++);
        grid.add(new Label("Notes"), 0, row); grid.add(notesArea, 1, row++);

        VBox root = new VBox(grid);
        root.setPrefWidth(440);
        pane.setContent(root);

        dlg.setResultConverter(bt -> {
            if (bt == null || bt.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            KnowledgeItem s = sourceBox.getValue();
            KnowledgeItem t = targetBox.getValue();
            if (s == null || t == null || s.getId() == t.getId()) return null;
            Connection c = new Connection(s.getId(), t.getId(), typeBox.getValue());
            c.setNotes(notesArea.getText());
            return c;
        });

        Optional<Connection> result = dlg.showAndWait();
        result.ifPresent(c -> {
            try {
                service.createConnection(
                        c.getSourceItemId(),
                        c.getTargetItemId(),
                        c.getRelationshipType(),
                        c.getNotes());
                AppContext.getInstance().setStatusMessage("Connection created.");
                loadGraph();
            } catch (ServiceException e) {
                Dialogs.error("Could not create connection", e.getMessage());
            }
        });
    }

    private ListCell<KnowledgeItem> itemCell() {
        return new ListCell<>() {
            @Override protected void updateItem(KnowledgeItem k, boolean empty) {
                super.updateItem(k, empty);
                if (empty || k == null) { setText(null); return; }
                setText(k.getItemType().getIcon() + "  " + k.getTitle());
            }
        };
    }

    private void deleteConnection(Connection c) {
        boolean ok = Dialogs.confirm("Delete connection?",
                "This will remove the connection between these two items. "
                        + "The knowledge items themselves are not deleted.");
        if (!ok) return;
        try {
            service.deleteConnection(c.getId());
            loadGraph();
            showEmptySidePanel();
        } catch (ServiceException e) {
            Dialogs.error("Could not delete connection", e.getMessage());
        }
    }

    // ------------------------------------------------------------- deep-link

    // GraphView is registered by SceneNavigator. A future deep-link from
    // other views could do: SceneNavigator.getInstance().navigateTo(GRAPH).

    static {
        // Static initializer is intentionally empty. Reserved for future deep-link
        // state if needed (e.g. "focus on this node id on next load").
    }

    @SuppressWarnings("unused")
    private static void safeRun(Runnable r) {
        Platform.runLater(r);
    }
}