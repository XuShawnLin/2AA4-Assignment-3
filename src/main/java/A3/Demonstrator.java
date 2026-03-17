package A3;

import java.util.List;
import java.util.logging.Logger;

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

    private static void aiBuildTurn(Player p, Board board, BuildValidator validator,
                                    BuildStructure buildService, int round) {

        boolean built = false;

        for (Node n : board.getNodes()) {
            if (validator.canBuildSettlement(p, n, board, false)
                    && buildService.buildSettlement(p, n, board)) {

                LOG.info(round + " / " + p.getName()
                        + ": built a settlement on node " + n.getId());

                built = true;
                break;
            }
        }

        if (!built) {

            for (Edge e : board.getEdges()) {
                if (board.isValidRoad(e, p)
                        && buildService.buildRoad(p, e, board)) {

                    LOG.info(round + " / " + p.getName()
                            + ": built a road on edge " + e.getId());

                    built = true;
                    break;
                }
            }
        }

        if (!built) {
            LOG.info(round + " / " + p.getName()
                    + ": ended turn (no valid build or not enough resources)");
        }
    }
}
