package com.mindmap.view.graph;

import com.mindmap.algorithm.ForceDirectedLayout;
import com.mindmap.model.Connection;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.RelationshipType;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Interactive graph canvas.
 *
 * <p>Uses a {@link Group} ("world") for zoom & pan, containing one
 * {@link StackPane} per node and one {@link Line} per edge. A force-directed
 * simulation runs on an {@link AnimationTimer}.</p>
 *
 * <p>Interactions:
 * <ul>
 *   <li><b>Drag a node</b> — moves it (node is pinned while dragging)</li>
 *   <li><b>Drag the background</b> — pans the canvas</li>
 *   <li><b>Scroll</b> — zooms in/out around the cursor</li>
 *   <li><b>Click a node</b> — fires the selection callback</li>
 *   <li><b>Double-click a node</b> — fires the "open details" callback</li>
 *   <li><b>Right-click a node</b> — fires the "context menu" callback</li>
 * </ul>
 * </p>
 */
public class GraphCanvas extends Pane {

    private static final Logger log = LoggerFactory.getLogger(GraphCanvas.class);

    // Sizing
    private static final double WORLD_WIDTH  = 2000;
    private static final double WORLD_HEIGHT = 1600;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;

    // Colors by item type
    private static final Map<String, String> TYPE_COLORS = Map.of(
            "CONCEPT",   "#3498DB",
            "FACT",      "#9B59B6",
            "SKILL",     "#27AE60",
            "PRINCIPLE", "#F39C12",
            "IDEA",      "#E91E63",
            "LESSON",    "#1ABC9C",
            "INSIGHT",   "#FF5722",
            "QUESTION",  "#795548"
    );

    private final Group world = new Group();

    private final Map<Integer, GraphNode> nodesById = new HashMap<>();
    private final Map<Integer, Line>      edgesById = new HashMap<>();

    private List<Connection> edges = new ArrayList<>();

    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private double dragStartX, dragStartY;
    private double dragOffsetStartX, dragOffsetStartY;
    private boolean panning;

    private AnimationTimer timer;
    private volatile boolean running;

    // Callbacks
    private Consumer<GraphNode> onNodeSelected;
    private Consumer<GraphNode> onNodeDoubleClick;
    private Consumer<GraphNode> onNodeRightClick;

    // Selection / highlight
    private GraphNode selected;

    public GraphCanvas() {
        setPrefSize(WORLD_WIDTH, WORLD_HEIGHT);
        setMinSize(400, 300);
        setStyle("-fx-background-color: -fx-bg-secondary;");

        getChildren().add(world);

        installPanAndZoom();
        installResizeHandler();
    }

    // ------------------------------------------------------------- public API

    public void setOnNodeSelected(Consumer<GraphNode> cb)      { this.onNodeSelected = cb; }
    public void setOnNodeDoubleClick(Consumer<GraphNode> cb)   { this.onNodeDoubleClick = cb; }
    public void setOnNodeRightClick(Consumer<GraphNode> cb)    { this.onNodeRightClick = cb; }

    /**
     * Rebuilds the whole graph from scratch.
     * @param items    all knowledge items (nodes)
     * @param edges    all connections (edges)
     * @param crossSource  map of itemId → source count (>=2) for badge rendering
     */
    public void loadGraph(List<KnowledgeItem> items,
                          List<Connection> edges,
                          Map<Integer, Integer> crossSource) {
        stopSimulation();
        world.getChildren().clear();
        nodesById.clear();
        edgesById.clear();
        this.edges = new ArrayList<>(edges);
        this.selected = null;

        if (items.isEmpty()) {
            Label empty = new Label("No knowledge items yet — add some to see your graph.");
            empty.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 14px;");
            empty.setLayoutX(60);
            empty.setLayoutY(80);
            world.getChildren().add(empty);
            return;
        }

        // Edges first (below nodes)
        for (Connection c : edges) {
            Line line = new Line();
            line.setStrokeWidth(1.6);
            Color color;
            try {
                color = Color.web(c.getRelationshipType().getColorHex());
            } catch (Exception ex) {
                color = Color.web("#95A5A6");
            }
            line.setStroke(color.deriveColor(0, 1, 1, 0.55));
            line.setMouseTransparent(true);
            edgesById.put(c.getId(), line);
            world.getChildren().add(line);
        }

        // Nodes
        int i = 0;
        int n = Math.max(1, items.size());
        for (KnowledgeItem item : items) {
            GraphNode node = createNode(item, i++, n, crossSource);
            nodesById.put(item.getId(), node);
            world.getChildren().add(node.view);
            attachNodeHandlers(node);
        }

        renderPositions();
        startSimulation();
    }

    /** Highlights the given node and its direct neighbors; dims everything else. */
    public void highlightNode(GraphNode node, Set<Integer> neighborIds) {
        this.selected = node;
        Set<Integer> focus = new HashSet<>();
        if (node != null) focus.add(node.getId());
        if (neighborIds != null) focus.addAll(neighborIds);

        for (GraphNode n : nodesById.values()) {
            double opacity = focus.isEmpty() || focus.contains(n.getId()) ? 1.0 : 0.25;
            n.view.setOpacity(opacity);
        }
        for (Map.Entry<Integer, Line> e : edgesById.entrySet()) {
            Connection c = findEdge(e.getKey());
            if (c == null) continue;
            boolean touchesSelection = node != null
                    && (c.getSourceItemId() == node.getId()
                    || c.getTargetItemId() == node.getId());
            boolean touchesFocus = !focus.isEmpty()
                    && (focus.contains(c.getSourceItemId())
                    || focus.contains(c.getTargetItemId()));
            e.getValue().setOpacity(focus.isEmpty() ? 0.55 : (touchesSelection ? 1.0 : (touchesFocus ? 0.7 : 0.15)));
            e.getValue().setStrokeWidth(touchesSelection ? 2.6 : 1.6);
        }
    }

    /** Clears any highlight/dimming. */
    public void clearHighlight() {
        this.selected = null;
        for (GraphNode n : nodesById.values()) n.view.setOpacity(1.0);
        for (Line l : edgesById.values()) {
            l.setOpacity(0.55);
            l.setStrokeWidth(1.6);
        }
    }

    /** Highlights (dims everyone else) all nodes whose title contains the query. */
    public void searchHighlight(String query) {
        if (query == null || query.isBlank()) {
            clearHighlight();
            return;
        }
        String q = query.toLowerCase();
        Set<Integer> matches = new HashSet<>();
        for (GraphNode n : nodesById.values()) {
            if (n.item.getTitle() != null && n.item.getTitle().toLowerCase().contains(q)) {
                matches.add(n.getId());
            }
        }
        for (GraphNode n : nodesById.values()) {
            n.view.setOpacity(matches.isEmpty() || matches.contains(n.getId()) ? 1.0 : 0.25);
        }
        for (Map.Entry<Integer, Line> e : edgesById.entrySet()) {
            Connection c = findEdge(e.getKey());
            if (c == null) continue;
            boolean hit = matches.contains(c.getSourceItemId())
                    || matches.contains(c.getTargetItemId());
            e.getValue().setOpacity(hit ? 1.0 : 0.15);
        }
    }

    /** Resets zoom and pan to identity. */
    public void resetView() {
        zoom = 1.0;
        offsetX = 0;
        offsetY = 0;
        applyTransform();
    }

    /** Fully re-runs the layout from a "warm" state. */
    public void rerunLayout() {
        for (GraphNode n : nodesById.values()) {
            n.vx = 0; n.vy = 0;
            // small nudge to break symmetry
            n.x += (Math.random() - 0.5) * 20;
            n.y += (Math.random() - 0.5) * 20;
        }
        startSimulation();
    }

    public GraphNode getSelected() { return selected; }
    public int getNodeCount() { return nodesById.size(); }
    public int getEdgeCount() { return edges.size(); }

    // ------------------------------------------------------------- node creation

    private GraphNode createNode(KnowledgeItem item, int index, int total,
                                 Map<Integer, Integer> crossSource) {

        double radius = switch (item.getImportance()) {
            case "HIGH" -> 34;
            case "LOW"  -> 22;
            default     -> 28;
        };

        String color = TYPE_COLORS.getOrDefault(item.getItemType().name(), "#3498DB");
        boolean isCrossSource = crossSource != null && crossSource.containsKey(item.getId());
        int sourceCount = isCrossSource ? crossSource.get(item.getId()) : 0;

        Circle circle = new Circle(radius);
        circle.setFill(Color.web(color));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        // Emoji icon on top of the circle
        Text icon = new Text(item.getItemType().getIcon());
        icon.setFont(Font.font("System", FontWeight.NORMAL, radius * 0.9));
        icon.setTextAlignment(TextAlignment.CENTER);
        icon.setFill(Color.WHITE);

        // Title label below the circle
        String title = item.getTitle() == null ? "" : item.getTitle();
        if (title.length() > 22) title = title.substring(0, 20) + "…";
        Text label = new Text(title);
        label.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        label.setFill(Color.web("#2C3E50"));
        label.setTextAlignment(TextAlignment.CENTER);

        // Small cross-source badge
        if (isCrossSource) {
            Circle badge = new Circle(9);
            badge.setFill(Color.web("#E74C3C"));
            badge.setStroke(Color.WHITE);
            badge.setStrokeWidth(1.5);
            badge.setLayoutX(radius * 0.75);
            badge.setLayoutY(-radius * 0.75);

            Text badgeText = new Text(String.valueOf(sourceCount));
            badgeText.setFont(Font.font("System", FontWeight.BOLD, 9));
            badgeText.setFill(Color.WHITE);
            badgeText.setLayoutX(radius * 0.75 - 3);
            badgeText.setLayoutY(-radius * 0.75 + 3.5);

            // Anchor icon + title vertically
            icon.setLayoutY(radius * 0.32);
            icon.setLayoutX(-radius * 0.28);

            // Add badge + icon as overlays inside a StackPane? Simpler: use a Group
            // but we need relative positioning for the drag handlers, so use StackPane.
            StackPane visual = new StackPane();
            visual.setPrefSize(radius * 2 + 60, radius * 2 + 30);
            visual.setMaxSize(StackPane.USE_PREF_SIZE, StackPane.USE_PREF_SIZE);

            // Construct the visual with absolute overlays
            Pane overlay = new Pane();
            overlay.setPrefSize(radius * 2 + 60, radius * 2 + 30);
            overlay.setMouseTransparent(true);

            circle.setLayoutX(radius + 30);
            circle.setLayoutY(radius + 15);
            icon.setLayoutX(radius + 30 - radius * 0.28);
            icon.setLayoutY(radius + 15 + radius * 0.32);

            badge.setLayoutX(radius + 30 + radius * 0.75);
            badge.setLayoutY(radius + 15 - radius * 0.75);
            badgeText.setLayoutX(radius + 30 + radius * 0.75 - 3);
            badgeText.setLayoutY(radius + 15 - radius * 0.75 + 3.5);

            label.setLayoutX(radius + 30 - 60);   // 60 = max label half-width
            label.setLayoutY(radius * 2 + 28);
            label.setWrappingWidth(120);
            label.setTextAlignment(TextAlignment.CENTER);

            overlay.getChildren().addAll(circle, icon, badge, badgeText, label);

            StackPane stack = new StackPane(overlay);
            stack.setStyle("-fx-cursor: hand;");

            GraphNode node = new GraphNode(item, stack);
            node.radius = radius;
            node.colorHex = color;

            // Position is managed via setLayoutX/Y on the StackPane
            node.x = 300 + (index * 90) % 900;
            node.y = 250 + (index * 70) % 600;

            return node;
        }

        // No cross-source badge — simpler path
        icon.setLayoutX(-radius * 0.28);
        icon.setLayoutY(radius * 0.32);

        label.setWrappingWidth(120);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setLayoutY(radius + 18);

        Pane overlay = new Pane();
        overlay.setPrefSize(radius * 2 + 60, radius * 2 + 30);
        overlay.setMouseTransparent(true);

        circle.setLayoutX(radius + 30);
        circle.setLayoutY(radius + 15);
        icon.setLayoutX(radius + 30 - radius * 0.28);
        icon.setLayoutY(radius + 15 + radius * 0.32);
        label.setLayoutX(radius + 30 - 60);
        label.setLayoutY(radius * 2 + 28);

        overlay.getChildren().addAll(circle, icon, label);

        StackPane stack = new StackPane(overlay);
        stack.setStyle("-fx-cursor: hand;");

        GraphNode node = new GraphNode(item, stack);
        node.radius = radius;
        node.colorHex = color;

        node.x = 300 + (index * 90) % 900;
        node.y = 250 + (index * 70) % 600;

        return node;
    }

    // ------------------------------------------------------------- interaction

    private void attachNodeHandlers(GraphNode node) {
        final double[] dragStart = new double[2];

        node.view.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dragStart[0] = e.getSceneX();
            dragStart[1] = e.getSceneY();
            node.pinned = true;
            node.vx = 0; node.vy = 0;
            e.consume();
        });

        node.view.setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            double dx = (e.getSceneX() - dragStart[0]) / zoom;
            double dy = (e.getSceneY() - dragStart[1]) / zoom;
            node.x += dx;
            node.y += dy;
            dragStart[0] = e.getSceneX();
            dragStart[1] = e.getSceneY();
            renderPositions();
            e.consume();
        });

        node.view.setOnMouseReleased(e -> {
            node.pinned = false;
            startSimulation();
        });

        node.view.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 1) {
                    if (onNodeSelected != null) onNodeSelected.accept(node);
                } else if (e.getClickCount() == 2) {
                    if (onNodeDoubleClick != null) onNodeDoubleClick.accept(node);
                }
            } else if (e.getButton() == MouseButton.SECONDARY) {
                if (onNodeRightClick != null) onNodeRightClick.accept(node);
            }
        });
    }

    private void installPanAndZoom() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getTarget() == this) {
                panning = true;
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                dragOffsetStartX = offsetX;
                dragOffsetStartY = offsetY;
                setStyle("-fx-background-color: -fx-bg-secondary; -fx-cursor: closed-hand;");
            }
        });

        setOnMouseDragged(e -> {
            if (panning) {
                offsetX = dragOffsetStartX + (e.getSceneX() - dragStartX);
                offsetY = dragOffsetStartY + (e.getSceneY() - dragStartY);
                applyTransform();
            }
        });

        setOnMouseReleased(e -> {
            panning = false;
            setStyle("-fx-background-color: -fx-bg-secondary;");
        });

        addEventHandler(ScrollEvent.SCROLL, e -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            double newZoom = zoom * factor;
            if (newZoom < MIN_ZOOM) newZoom = MIN_ZOOM;
            if (newZoom > MAX_ZOOM) newZoom = MAX_ZOOM;
            if (Math.abs(newZoom - zoom) < 0.0001) return;

            double mx = e.getX();
            double my = e.getY();
            double scale = newZoom / zoom;
            offsetX = mx - scale * (mx - offsetX);
            offsetY = my - scale * (my - offsetY);
            zoom = newZoom;
            applyTransform();
            e.consume();
        });
    }

    private void installResizeHandler() {
        widthProperty().addListener((o, ov, nv) -> {
            if (nv.doubleValue() > 0) {
                world.setLayoutX(0);
                world.setLayoutY(0);
            }
        });
    }

    private void applyTransform() {
        world.setScaleX(zoom);
        world.setScaleY(zoom);
        world.setTranslateX(offsetX);
        world.setTranslateY(offsetY);
    }

    // ------------------------------------------------------------- rendering

    /** Updates each node's layout position and redraws every edge. */
    private void renderPositions() {
        for (GraphNode n : nodesById.values()) {
            Bounds b = n.view.getBoundsInLocal();
            double offsetCorrection = 30 + n.radius; // center offset within the StackPane
            n.view.setLayoutX(n.x - offsetCorrection);
            n.view.setLayoutY(n.y - offsetCorrection);
        }
        for (Map.Entry<Integer, Line> e : edgesById.entrySet()) {
            Connection c = findEdge(e.getKey());
            if (c == null) continue;
            GraphNode a = nodesById.get(c.getSourceItemId());
            GraphNode b = nodesById.get(c.getTargetItemId());
            if (a == null || b == null) continue;
            Line line = e.getValue();
            line.setStartX(a.x);
            line.setStartY(a.y);
            line.setEndX(b.x);
            line.setEndY(b.y);
        }
    }

    private Connection findEdge(int id) {
        for (Connection c : edges) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    // ------------------------------------------------------------- simulation

    private void startSimulation() {
        if (running) return;
        running = true;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                boolean moving = ForceDirectedLayout.step(
                        nodesById, edges,
                        WORLD_WIDTH, WORLD_HEIGHT);
                renderPositions();
                if (!moving) {
                    stop();
                    running = false;
                    log.debug("Force layout settled ({} nodes, {} edges)",
                            nodesById.size(), edges.size());
                }
            }
        };
        timer.start();
    }

    private void stopSimulation() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        running = false;
    }

    /** Called by the owner view when this canvas is hidden. */
    public void pause() { stopSimulation(); }
}