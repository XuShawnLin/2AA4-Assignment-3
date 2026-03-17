package A3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal JSON exporter that converts the Java board state into the
 * JSON schema expected by visualize/light_visualizer.py.
 *
 * R2.2/R2.3: Maintain external JSON state and integrate with the Python visualizer.
 */
public final class VisualExporter {
    private VisualExporter() {}

    // Persistent watcher process for Python visualizer (--watch mode)
    private static volatile Process watchProcess;

    private static final Logger LOGGER = Logger.getLogger(VisualExporter.class.getName());

    // Spiral order cube coordinates for a radius-2 hex grid (center, ring1, ring2)
    // Matches Board tile ids: 0=center, 1-6=inner ring, 7-18=outer ring
    // Each triplet is (q, s, r) with q + s + r == 0
    private static final int[][] TILE_CUBE_COORDS = new int[][]{
            { 0,  0,  0}, // 0 center
            { 1, -1,  0}, // 1 ring1 (east)
            { 1,  0, -1}, // 2
            { 0,  1, -1}, // 3
            {-1,  1,  0}, // 4
            {-1,  0,  1}, // 5
            { 0, -1,  1}, // 6
            // ring2 clockwise starting east
            { 2, -2,  0}, // 7
            { 2, -1, -1}, // 8
            { 1,  1, -2}, // 9
            { 0,  2, -2}, // 10
            {-1,  2, -1}, // 11
            {-2,  2,  0}, // 12
            {-2,  1,  1}, // 13
            {-2,  0,  2}, // 14
            {-1, -1,  2}, // 15
            { 0, -2,  2}, // 16
            { 1, -2,  1}, // 17
            { 2,  0, -2}  // 18
    };

    private static String toVisualizerResource(ResourceType r) {
        if (r == null) return "DESERT";
        return switch (r) {
            case LUMBER -> "WOOD";
            case GRAIN -> "WHEAT";
            case WOOL -> "SHEEP";
            case BRICK -> "BRICK";
            case ORE -> "ORE";
        };
    }

    private static String toVisualizerBuilding(BuildingType b) {
        if (b == null) return null;
        return switch (b) {
            case SETTLEMENT -> "SETTLEMENT";
            case CITY -> "CITY";
            case ROAD -> "ROAD"; // Only used on edges
        };
    }

    private static String toVisualizerColor(Player p, Player[] players) {
        // Deterministic mapping by seating order
        int idx = -1;
        for (int i = 0; i < players.length; i++) {
            if (players[i] == p) { idx = i; break; }
        }
        return switch (idx) {
            case 0 -> "RED";
            case 1 -> "BLUE";
            case 2 -> "ORANGE";
            case 3 -> "WHITE";
            default -> "RED"; // fallback
        };
    }

    public static void export(Board board, Player[] players, boolean renderPng) {
        // Resolve visualize directory under project root
        String root = System.getProperty("user.dir");
        File visualizeDir = new File(root, "visualize");
        File scraped = new File(visualizeDir, "scraped_boards");
        if (!scraped.exists() && !scraped.mkdirs()) {
            LOGGER.warning("[Visualizer] Failed to create scraped_boards directory; images may not be saved.");
        }

        File baseMap = new File(visualizeDir, "base_map.json");
        File state = new File(visualizeDir, "state.json");

        try {
            writeBaseMap(board, baseMap);
            writeState(board, players, state);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[Visualizer] Failed to write JSON: {0}", e.getMessage());
            return;
        }

        // Prefer watch mode: callers can set renderPng=false and start watcher once.
        if (renderPng) {
            runPythonRender(visualizeDir, baseMap.getName(), state.getName());
        }
    }

    private static void writeBaseMap(Board board, File file) throws IOException {
        List<HexTile> tiles = board.getTiles();
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\n  \"tiles\": [\n");
        for (int i = 0; i < Math.min(tiles.size(), 19); i++) {
            HexTile t = tiles.get(i);
            int[] c = TILE_CUBE_COORDS[i];
            String res = toVisualizerResource(t.getResource());
            Integer num = t.getTokenNumber();
            if (i > 0) sb.append(",\n");
            sb.append("    {")
              .append("\"q\": ").append(c[0]).append(", ")
              .append("\"s\": ").append(c[1]).append(", ")
              .append("\"r\": ").append(c[2]).append(", ")
              .append("\"resource\": \"").append(res).append("\"");
            if (!Objects.equals(res, "DESERT") && num != null) {
                sb.append(", \"number\": ").append(num);
            }
            sb.append("}");
        }
        sb.append("\n  ]\n}");

        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            w.write(sb.toString());
        }
    }

    private static void writeState(Board board, Player[] players, File file) throws IOException {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("{\n  \"buildings\": [\n");

        boolean first = true;
        // Pre-compute number of roads per player and quick connectivity lookup
        java.util.Map<Player, Integer> roadCount = new java.util.HashMap<>();
        java.util.Map<Player, java.util.Set<Integer>> roadTouchingNodes = new java.util.HashMap<>();
        for (Edge e : board.getEdges()) {
            if (e.getOwner() != null && e.getBuilding() == BuildingType.ROAD && e.getConnectedNodes().size() == 2) {
                roadCount.merge(e.getOwner(), 1, Integer::sum);
                roadTouchingNodes.computeIfAbsent(e.getOwner(), k -> new java.util.HashSet<>())
                        .add(e.getConnectedNodes().get(0).getId());
                roadTouchingNodes.computeIfAbsent(e.getOwner(), k -> new java.util.HashSet<>())
                        .add(e.getConnectedNodes().get(1).getId());
            }
        }

        // Keep track of how many initial settlements we export per player (when there are no roads)
        java.util.Map<Player, Integer> exportedInitialSettlements = new java.util.HashMap<>();
        // Additionally, if there are zero roads overall, be extra conservative and export
        // at most two settlements per player (Catan initial placements) when absolutely no
        // roads exist yet. This aligns with the visualizer's ability to accept initial
        // placements without connectivity.
        int totalRoadsAllPlayers = 0;
        for (Integer c : roadCount.values()) totalRoadsAllPlayers += c;

        // Nodes -> settlements/cities
        for (Node n : board.getNodes()) {
            if (n.getOwner() == null || n.getBuilding() == null) continue;

            int nodeId = n.getId();
            if (nodeId < 0 || nodeId >= 54) continue; // catanatron expects 0..53 land nodes

            BuildingType bt = n.getBuilding();
            Player owner = n.getOwner();

            boolean include = true;
            if (bt == BuildingType.SETTLEMENT) {
                int roads = roadCount.getOrDefault(owner, 0);
                if (roads == 0) {
                    // Allow up to two initial placements per player even when there are no roads yet
                    int cap = 2;
                    int used = exportedInitialSettlements.getOrDefault(owner, 0);
                    if (used >= cap) {
                        include = false; // skip extras to avoid invalid non-connected placements
                    } else {
                        exportedInitialSettlements.put(owner, used + 1);
                    }
                } else {
                    // Require at least one owned road touching this node
                    java.util.Set<Integer> touch = roadTouchingNodes.getOrDefault(owner, java.util.Collections.emptySet());
                    include = touch.contains(nodeId);
                }
            }

            if (!include) continue;

            if (!first) sb.append(",\n");
            first = false;
            sb.append("    {")
              .append("\"node\": ").append(nodeId).append(", ")
              .append("\"owner\": \"").append(toVisualizerColor(owner, players)).append("\",")
              .append(" \"type\": \"").append(toVisualizerBuilding(bt)).append("\"")
              ;

            // Add an explicit initial flag to help external consumers (optional for current visualizer)
            if (bt == BuildingType.SETTLEMENT && roadCount.getOrDefault(owner, 0) == 0) {
                sb.append(", \"initial\": true");
            }
            sb.append("}");
        }

        sb.append("\n  ],\n  \"roads\": [\n");

        first = true;
        for (Edge e : board.getEdges()) {
            if (e.getOwner() != null && e.getBuilding() == BuildingType.ROAD && e.getConnectedNodes().size() == 2) {
                if (!first) sb.append(",\n");
                first = false;
                int a = e.getConnectedNodes().get(0).getId();
                int b = e.getConnectedNodes().get(1).getId();
                sb.append("    {")
                  .append("\"a\": ").append(a).append(", ")
                  .append("\"b\": ").append(b).append(", ")
                  .append("\"owner\": \"").append(toVisualizerColor(e.getOwner(), players)).append("\"")
                  .append("}");
            }
        }

        sb.append("\n  ]\n}");

        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            w.write(sb.toString());
        }
    }

    private static void runPythonRender(File visualizeDir, String baseMapName, String stateName) {
        // Strictly allow only expected filenames to avoid command injection via parameters
        if (!"base_map.json".equals(baseMapName) || !"state.json".equals(stateName)) {
            LOGGER.warning("[Visualizer] Skipping render due to unexpected filenames.");
            return;
        }

        // Resolve python executable from environment when available, else fall back to 'python'
        String pyFromEnv = System.getenv("PYTHON");
        String pythonExe = (pyFromEnv != null && !pyFromEnv.isBlank()) ? pyFromEnv : "python";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExe,
                    "light_visualizer.py",
                    baseMapName,
                    stateName
            );
            pb.directory(visualizeDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Wait with timeout; if exceeds, destroy the process
            boolean finished = p.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                LOGGER.warning("[Visualizer] Renderer timed out and was terminated.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "[Visualizer] Render interrupted: {0}", ie.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[Visualizer] Python render skipped: {0}", ex.getMessage());
        }
    }

    /**
     * Ensure the Python visualizer is running in watch mode. This will render a PNG
     * automatically whenever state.json changes. It launches once per JVM and
     * is terminated on JVM shutdown.
     */
    public static synchronized void ensureWatchRunning() {
        // Already running and alive
        if (watchProcess != null) {
            try {
                if (watchProcess.isAlive()) return;
            } catch (Exception ignored) { /* fallthrough to restart */ }
        }

        String root = System.getProperty("user.dir");
        File visualizeDir = new File(root, "visualize");

        // Resolve python executable from environment when available, else fall back to 'python'
        String pyFromEnv = System.getenv("PYTHON");
        String pythonExe = (pyFromEnv != null && !pyFromEnv.isBlank()) ? pyFromEnv : "python";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExe,
                    "light_visualizer.py",
                    "base_map.json",
                    "--watch"
            );
            pb.directory(visualizeDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            watchProcess = p;

            // Stop on JVM shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (watchProcess != null && watchProcess.isAlive()) {
                        watchProcess.destroy();
                        try {
                            if (!watchProcess.waitFor(2, TimeUnit.SECONDS)) {
                                watchProcess.destroyForcibly();
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception ignored) { }
            }, "VisualizerWatchShutdown"));

            LOGGER.info("[Visualizer] Watch mode started (light_visualizer.py --watch).");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[Visualizer] Failed to start watch mode: {0}", ex.getMessage());
        }
    }
}
