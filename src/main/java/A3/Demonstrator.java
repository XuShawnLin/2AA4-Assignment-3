package A3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

public class Demonstrator {
    
    private static final Logger LOG = Logger.getLogger(Demonstrator.class.getName());

    public static void main(String[] args) {
        Args cfg = parseArgs(args);
        runGame(cfg);
    }

    private static Args parseArgs(String[] args) {
        int maxTurns = 8192;
        boolean useWatch = false;

        if (args != null) {
            for (String arg : args) {
                if ("--watch".equalsIgnoreCase(arg)) {
                    useWatch = true;
                } else {
                    try {
                        int inputTurns = Integer.parseInt(arg);
                        if (inputTurns > 0 && inputTurns <= 8192) {
                            maxTurns = inputTurns;
                        }
                    } catch (NumberFormatException nfe) {
                        LOG.warning("[Config] Ignoring unrecognized argument '" + arg + "': " + nfe.getMessage());
                    }
                }
            }
        }

        LOG.info("Turns: " + maxTurns + (useWatch ? " (watch mode)" : ""));
        return new Args(maxTurns, useWatch);
    }

    private static void runGame(Args cfg) {
        GameMaster gameMaster = new GameMaster();
        gameMaster.startGame();

        Player[] players = createPlayers();
        gameMaster.setPlayers(players);

        Board board = gameMaster.getBoard();
        BuildStructure buildService = gameMaster.getBuildService();
        BuildValidator validator = buildService.getValidator();

        setupTiles(board);

        initVisualizer(board, players, cfg.useWatch);

        doInitialPlacements(players, board, validator, cfg.useWatch);

        playRounds(cfg.maxTurns, gameMaster, board, players, validator, buildService, cfg.useWatch);
    }

    private static Player[] createPlayers() {
        return new Player[]{
                new HumanPlayer("Shawn"),
                new Player("Sabrina"),
                new Player("Subha"),
                new Player("Ahmed")
        };
    }

    private static void initVisualizer(Board board, Player[] players, boolean useWatch) {
        if (useWatch) {
            VisualExporter.export(board, players, false);
            VisualExporter.ensureWatchRunning();
        } else {
            VisualExporter.export(board, players, true);
        }
    }

    private static void doInitialPlacements(Player[] players, Board board, BuildValidator validator, boolean useWatch) {
        for (Player p : players) {
            Node node = chooseInitialNode(p, board, validator);
            placeSettlement(p, node, "first");
            VisualExporter.export(board, players, !useWatch);
        }

        for (int i = players.length - 1; i >= 0; i--) {
            Player p = players[i];
            Node node = chooseInitialNode(p, board, validator);
            placeSettlement(p, node, "second");
            VisualExporter.export(board, players, !useWatch);
        }
    }

    private static void playRounds(int maxTurns,
                                   GameMaster gameMaster,
                                   Board board,
                                   Player[] players,
                                   BuildValidator validator,
                                   BuildStructure buildService,
                                   boolean useWatch) {

        for (int round = 1; round <= maxTurns; round++) {
            LOG.info("Turn: " + round);

            for (int i = 0; i < players.length; i++) {
                Player p = gameMaster.getCurrentPlayer();
                LOG.info("----- " + p.getName() + "'s Turn -----");

                handleDiceRoll(gameMaster, board, players, p, round);

                if (p instanceof HumanPlayer human) {
                    human.takeTurn(gameMaster, round);
                } else {
                    aiBuildTurn(p, board, validator, buildService, round);
                }

                VisualExporter.export(board, players, !useWatch);

                if (gameMaster.checkWin()) {
                    LOG.info(round + " / " + p.getName() + ": WON THE GAME!");
                    return;
                }

                gameMaster.nextTurn();
            }
        }
    }

    private record Args(int maxTurns, boolean useWatch) {}

    //Set up board method
    private static void setupTiles(Board board) {

        ResourceType[] resOrder = {
                ResourceType.LUMBER, ResourceType.LUMBER, ResourceType.LUMBER, ResourceType.LUMBER,
                ResourceType.GRAIN, ResourceType.GRAIN, ResourceType.GRAIN,
                ResourceType.GRAIN, ResourceType.WOOL, ResourceType.WOOL, ResourceType.WOOL,
                ResourceType.WOOL, ResourceType.BRICK, ResourceType.BRICK, ResourceType.BRICK,
                ResourceType.ORE, ResourceType.ORE, ResourceType.ORE,
                null
        };

        int[] tokenOrder = {11,3,6,4,5,9,10,8,2,12,9,10,4,5,6,3,8,11,0};

        List<HexTile> tiles = board.getTiles();

        for (int i = 0; i < tiles.size() && i < 19; i++) {

            HexTile tile = tiles.get(i);

            tile.setResource(resOrder[i]);

            if (tokenOrder[i] != 0)
                tile.setTokenNumber(tokenOrder[i]);

            for (int j = 0; j < 3; j++) {
                int nodeId = (i * 2 + j) % board.getNodes().size();
                tile.addNode(board.getNodes().get(nodeId));
            }
        }
    }

    //Method for placing settlements on Nodes
    private static Node chooseInitialNode(Player p, Board board, BuildValidator validator) {

        Node node = null;

        if (p instanceof HumanPlayer human) {

            node = human.chooseInitialNode(board);

            if (!board.isValidSettlement(node, p, true)) {
                LOG.warning("Invalid node chosen, pick again.");
                node = human.chooseInitialNode(board);
            }

        } else {

            for (Node n : board.getNodes()) {

                if (validator.canBuildSettlement(p, n, board, true)) {
                    node = n;
                    break;
                }
            }
        }

        return node;
    }

    private static void placeSettlement(Player p, Node node, String order) {

        node.setOwner(p);
        node.setBuilding(BuildingType.SETTLEMENT);
        p.addVictoryPoints(1);

        LOG.info("0 / " + p.getName() + ": placed " + order +
                " settlement on node " + node.getId());
    }

    private static void handleDiceRoll(GameMaster gameMaster, Board board,
                                       Player[] players, Player p, int round) {

        if (p instanceof HumanPlayer) {
            return;
        }

        int roll = gameMaster.rollDice();

        LOG.info(round + " / " + p.getName() + ": rolled a " + roll);

        if (roll == 7) {

            new Robber().rollSeven(board, players, p);

            LOG.info(round + " / " + p.getName() + ": robber activated");

        } else {

            gameMaster.distributeResources(roll);

        }
    }

    private static void aiBuildTurn(Player p, Board board,
                                BuildValidator validator,
                                BuildStructure buildService, int round) {

        // Define the AI rules to evaluate
        List<Rule> rules = List.of(
                // Build settlement rule; random tie-breaking will be safe
                new BuildSettlementRule(validator),
                new BuildRoadRule(),
                new SpendCardsRule()
        );

        double bestValue = -1;
        List<Rule> bestRules = new ArrayList<>();
    
        // Evaluate all rules to find the best ones
        for (Rule r : rules) {
            double val = r.evaluate(p, board);
    
            if (val > bestValue) {
                bestValue = val;
                bestRules.clear();
                bestRules.add(r);
            } else if (val == bestValue) {
                bestRules.add(r);
            }
        }

        // Tie-breaking: choose a random rule among the best rules
        Rule chosen = chooseRandomRule(bestRules);
    
        if (chosen != null) {
            // Apply the chosen rule; if it fails, log a fine-level message
            boolean success = chosen.apply(p, board, buildService, round);
            if (!success) {
                LOG.fine("No valid action executed for " + p.getName());
            }
        } else {
            // No valid rules available; AI skips turn silently
            LOG.fine(p.getName() + " has no actions this turn.");
        }
    }

    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe here
    private static Rule chooseRandomRule(List<Rule> bestRules) {
        if (bestRules.isEmpty()) {
            return null;
        }
        return bestRules.get(ThreadLocalRandom.current().nextInt(bestRules.size()));
    }

    // ------------------------ Helper methods to enforce constraints ------------------------

    protected static boolean trySpendDownToLimit(Player p, Board board, BuildValidator validator,
                                               BuildStructure buildService, int round, int limit) {
        boolean didSpend = false;
        // Prefer buying connected roads; fall back to settlements
        while (p.getTotalResources() > limit) {
            boolean boughtSomething = false;

            if (tryBuildAnyConnectedRoad(p, board, buildService, round)) {
                boughtSomething = true;
                didSpend = true;
            } else {
                // Try any valid settlement we can afford
                boolean builtSet = false;
                for (Node n : board.getNodes()) {
                    if (validator.canBuildSettlement(p, n, board, false)
                            && buildService.buildSettlement(p, n, board)) {
                        LOG.info(round + " / " + p.getName() + ": (spend>" + limit + ") built a settlement on node " + n.getId());
                        builtSet = true;
                        didSpend = true;
                        break;
                    }
                }
                boughtSomething = builtSet;
            }

            if (!boughtSomething) break; // Can't spend further
        }
        return didSpend;
    }

    private static boolean tryBuildAnyConnectedRoad(Player p, Board board, BuildStructure buildService, int round) {
        for (Edge e : board.getEdges()) {
            if (!e.isOccupied() && board.isValidRoad(e, p) && buildService.buildRoad(p, e, board)) {
                LOG.info(round + " / " + p.getName() + ": built a road on edge " + e.getId());
                return true;
            }
        }
        return false;
    }

    private static boolean tryConnectSegmentsWithinTwo(Player p, Board board, BuildStructure buildService, int round) {
        // Build up to two roads that reduce distance between two of our road components
        // Heuristic: attempt to place one valid connected road, then re-evaluate if another immediate placement now connects two components
        boolean builtAny = false;

        // First try a single connected road that increases our reach
        if (tryBuildAnyConnectedRoad(p, board, buildService, round)) {
            builtAny = true;
        }

        // If we can still afford and there exists another newly connected road, try one more
        if (builtAny) {
            if (tryBuildAnyConnectedRoad(p, board, buildService, round)) {
                builtAny = true;
            }
        }

        return builtAny;
    }

    private static int computeBestOpponentLongestRoad(Board board, Player me) {
        int best = -1;
        // We don't have direct access to all players here; approximate by scanning owners on edges
        // Collect distinct owners from edges
        // If the board has no edges set up, return -1 to skip
        for (Edge e : board.getEdges()) {
            Player owner = e.getOwner();
            if (owner != null && owner != me) {
                int len = computeLongestRoad(board, owner);
                if (len > best) best = len;
            }
        }
        return best;
    }

    private static int computeLongestRoad(Board board, Player p) {
        // DFS over player's owned edges; longest simple path without reusing an edge
        int longest = 0;
        for (Edge e : board.getEdges()) {
            if (e.getOwner() == p) {
                // Start DFS from both ends of the edge
                for (Node start : e.getConnectedNodes()) {
                    longest = Math.max(longest, dfsRoadLength(p, e, start, board));
                }
            }
        }
        return longest;
    }

    private static int dfsRoadLength(Player p, Edge startEdge, Node currentNode, Board board) {
        // Depth-first without reusing edges; use recursion with visited edges tracking (implemented via parameter passing)
        return dfsFromNode(p, currentNode, startEdge, 0);
    }

    private static int dfsFromNode(Player p, Node node, Edge cameFrom, int length) {
        int best = length + 1; // count current edge
        for (Edge next : node.getConnectedEdges()) {
            if (next == cameFrom) continue;
            if (next.getOwner() != p) continue;
            // Move to the opposite node of next
            Node nextNode = null;
            for (Node n : next.getConnectedNodes()) {
                if (n != node) { nextNode = n; break; }
            }
            if (nextNode == null) continue;
            best = Math.max(best, dfsFromNode(p, nextNode, next, length + 1));
        }
        return best;
    }
}
