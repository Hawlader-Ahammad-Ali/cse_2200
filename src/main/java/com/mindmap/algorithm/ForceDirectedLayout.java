package com.mindmap.algorithm;

import com.mindmap.model.Connection;
import com.mindmap.view.graph.GraphNode;

import java.util.List;
import java.util.Map;

/**
 * Simple Fruchterman-Reingold-style force-directed layout.
 *
 * <p>Two forces are computed each step:
 * <ul>
 *   <li><b>Repulsion</b> between all pairs (inverse-square)</li>
 *   <li><b>Spring attraction</b> along edges (linear to rest length)</li>
 * </ul>
 * Plus a small pull toward the canvas center and velocity damping.</p>
 *
 * <p>The canvas drives this by calling {@link #step} from an
 * {@code AnimationTimer}. Returns {@code true} while still moving.</p>
 */
public final class ForceDirectedLayout {

    public static final double REPULSION     = 12000;
    public static final double SPRING_LENGTH = 180;
    public static final double SPRING_K      = 0.02;
    public static final double CENTER_PULL   = 0.008;
    public static final double DAMPING       = 0.82;
    public static final double MIN_ENERGY    = 0.15;
    public static final double MAX_SPEED     = 25.0;

    private ForceDirectedLayout() { }

    /**
     * Advances the simulation by one step and applies position updates.
     * @return true if any node is still in meaningful motion.
     */
    public static boolean step(Map<Integer, GraphNode> nodesById,
                               List<Connection> edges,
                               double width,
                               double height) {

        double cx = width / 2.0;
        double cy = height / 2.0;

        // Reset forces
        for (GraphNode n : nodesById.values()) {
            n.fx = 0;
            n.fy = 0;
        }

        // Pairwise repulsion
        GraphNode[] arr = nodesById.values().toArray(new GraphNode[0]);
        int n = arr.length;
        for (int i = 0; i < n; i++) {
            GraphNode a = arr[i];
            for (int j = i + 1; j < n; j++) {
                GraphNode b = arr[j];
                double dx = a.x - b.x;
                double dy = a.y - b.y;
                double d2 = dx * dx + dy * dy;
                if (d2 < 1) d2 = 1;
                double d = Math.sqrt(d2);
                double force = REPULSION / d2;
                double fx = (dx / d) * force;
                double fy = (dy / d) * force;
                a.fx += fx; a.fy += fy;
                b.fx -= fx; b.fy -= fy;
            }
        }

        // Spring attraction along edges
        for (Connection c : edges) {
            GraphNode a = nodesById.get(c.getSourceItemId());
            GraphNode b = nodesById.get(c.getTargetItemId());
            if (a == null || b == null) continue;
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double d = Math.sqrt(dx * dx + dy * dy) + 0.01;
            double delta = d - SPRING_LENGTH;
            double force = SPRING_K * delta;
            double fx = (dx / d) * force;
            double fy = (dy / d) * force;
            a.fx += fx; a.fy += fy;
            b.fx -= fx; b.fy -= fy;
        }

        // Center gravity
        for (GraphNode node : arr) {
            node.fx += (cx - node.x) * CENTER_PULL;
            node.fy += (cy - node.y) * CENTER_PULL;
        }

        // Integrate
        boolean moving = false;
        double minRadius = 40;
        for (GraphNode node : arr) {
            if (node.pinned) continue;

            node.vx = (node.vx + node.fx) * DAMPING;
            node.vy = (node.vy + node.fy) * DAMPING;

            // Cap speed to prevent explosions
            double speed = Math.hypot(node.vx, node.vy);
            if (speed > MAX_SPEED) {
                double s = MAX_SPEED / speed;
                node.vx *= s;
                node.vy *= s;
            }

            node.x += node.vx;
            node.y += node.vy;

            // Soft clamp to canvas
            node.x = Math.max(minRadius, Math.min(width  - minRadius, node.x));
            node.y = Math.max(minRadius, Math.min(height - minRadius, node.y));

            if (Math.abs(node.vx) + Math.abs(node.vy) > MIN_ENERGY) {
                moving = true;
            }
        }
        return moving;
    }
}