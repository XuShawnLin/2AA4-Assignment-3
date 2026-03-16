package A2;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Human-controlled player.
 * Allows interactive commands for rolling, building, and viewing resources.
 */
public class HumanPlayer extends Player {

    private final Scanner scanner;
    private static final Logger LOGGER = Logger.getLogger(HumanPlayer.class.getName());

    public HumanPlayer(String name) {
        super(name);
        this.scanner = new Scanner(System.in);
    }

    /**
     * Let the human choose the initial settlement during game setup.
     */
    public Node chooseInitialNode(Board board) {
        Node chosenNode = null;

        while (chosenNode == null) {
            LOGGER.info("Choose a node id to place your settlement:");
            
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) return null;

            try {
                int nodeId = Integer.parseInt(input);
                Node node = board.getNodes().stream()
                        .filter(n -> n.getId() == nodeId)
                        .findFirst()
                        .orElse(null);

                if (node != null && board.isValidSettlement(node, this, true)) {
                    chosenNode = node;
                } else {
                    LOGGER.info("Invalid node. Try again.");
                }
            } catch (NumberFormatException e) {
                LOGGER.info("Please enter a valid number.");
            }
        }

        return chosenNode;
    }

    /**
     * Perform a human player's turn.
     */
    @Override
    public void takeTurn(GameMaster gameMaster, int round) {
        boolean rolled = false;
        boolean turnFinished = false;


        while (!turnFinished) {
            LOGGER.info("Command (Roll, Build, List, Go): ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "roll":
                    if (!rolled) {
                        int roll = gameMaster.rollDice();
                        LOGGER.log(Level.INFO, "{0} / {1}: rolled a {2}", new Object[]{round, getName(), roll});
                        if (roll == 7) {
                            LOGGER.info("Robber activated");
                            // robber logic can go here
                        } else {
                            gameMaster.distributeResources(roll);
                        }
                        rolled = true;
                    } else {
                        LOGGER.info("You already rolled this turn.");
                    }
                    break;

                case "list":
                    LOGGER.info("Your resources:");
                    LOGGER.info(getCurrentResources().toString());
                    break;

                case "build":
                    if (!rolled) {
                        LOGGER.info("You must roll first.");
                        break;
                    }
                    buildAction(gameMaster);
                    break;

                case "go":
                    if (!rolled) {
                        LOGGER.info("You must roll first.");
                    } else {
                        LOGGER.log(Level.INFO, "{0} / {1}: ended turn", new Object[]{round, getName()});
                        turnFinished = true;
                    }
                    break;

                default:
                    LOGGER.info("Unknown command.");
            }
        }
    }

    /**
     * Build logic for human player with resource check and back option.
     */
    private void buildAction(GameMaster gameMaster) {
        BuildStructure buildService = gameMaster.getBuildService();
        Board board = gameMaster.getBoard();

        LOGGER.info("What do you want to build? (Settlement, Road, City) or 'back': ");
        String type = scanner.nextLine().trim().toLowerCase();

        switch (type) {
            case "settlement":
                // Check resources: 1 Brick + 1 Lumber + 1 Wool + 1 Grain
                if (getCurrentResources().getOrDefault(ResourceType.BRICK, 0) < 1 ||
                        getCurrentResources().getOrDefault(ResourceType.LUMBER, 0) < 1 ||
                        getCurrentResources().getOrDefault(ResourceType.WOOL, 0) < 1 ||
                        getCurrentResources().getOrDefault(ResourceType.GRAIN, 0) < 1) {
                    LOGGER.info("Not enough resources to build a settlement.");
                    return;
                }

                Node settlementNode = chooseBuildNode(board, false); // normal gameplay
                if (settlementNode == null) return; // back option

                if (buildService.buildSettlement(this, settlementNode, board)) {
                    removeResource(ResourceType.BRICK, 1);
                    removeResource(ResourceType.LUMBER, 1);
                    removeResource(ResourceType.WOOL, 1);
                    removeResource(ResourceType.GRAIN, 1);
                    LOGGER.log(Level.INFO, "{0}: built a settlement on node {1}", new Object[]{getName(), settlementNode.getId()});
                } else {
                    LOGGER.info("Cannot build settlement there.");
                }
                break;

            case "city":
                // Check resources: 2 Grain + 3 Ore
                if (getCurrentResources().getOrDefault(ResourceType.GRAIN, 0) < 2 ||
                        getCurrentResources().getOrDefault(ResourceType.ORE, 0) < 3) {
                    LOGGER.info("Not enough resources to build a city.");
                    return;
                }

                Node cityNode = chooseBuildNode(board, false); // normal gameplay
                if (cityNode == null) return; // back option

                if (buildService.buildCity(this, cityNode, board)) {
                    removeResource(ResourceType.GRAIN, 2);
                    removeResource(ResourceType.ORE, 3);
                    LOGGER.log(Level.INFO, "{0}: built a city on node {1}", new Object[]{getName(), cityNode.getId()});
                } else {
                    LOGGER.info("Cannot build city there.");
                }
                break;

            case "road":
                // Check resources: 1 Brick + 1 Lumber
                if (getCurrentResources().getOrDefault(ResourceType.BRICK, 0) < 1 ||
                        getCurrentResources().getOrDefault(ResourceType.LUMBER, 0) < 1) {
                    LOGGER.info("Not enough resources to build a road.");
                    return;
                }

                Edge edge = chooseBuildEdge(board);
                if (edge == null) return; // back option

                if (buildService.buildRoad(this, edge, board)) {
                    removeResource(ResourceType.BRICK, 1);
                    removeResource(ResourceType.LUMBER, 1);
                    LOGGER.log(Level.INFO, "{0}: built a road on edge {1}", new Object[]{getName(), edge.getId()});
                } else {
                    LOGGER.info("Cannot build road there.");
                }
                break;

            case "back":
                LOGGER.info("Build canceled.");
                break;

            default:
                LOGGER.info("Unknown build type.");
                break;
        }
    }

    // Choose node to build (normal gameplay or initial placement)
    private Node chooseBuildNode(Board board, boolean initialPlacement) {
        Node chosenNode = null;
        while (chosenNode == null) {
            System.out.print("Enter node id or 'back': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) return null;

            try {
                int nodeId = Integer.parseInt(input);
                Node node = board.getNodes().stream()
                        .filter(n -> n.getId() == nodeId)
                        .findFirst()
                        .orElse(null);

                if (node != null && board.isValidSettlement(node, this, initialPlacement)) {
                    chosenNode = node;
                } else {
                    System.out.println("Cannot build there. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        return chosenNode;
    }

    private Edge chooseBuildEdge(Board board) {
        Edge chosenEdge = null;
        while (chosenEdge == null) {
            System.out.print("Enter edge id or 'back': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("back")) return null;

            try {
                int edgeId = Integer.parseInt(input);
                Edge edge = board.getEdges().stream()
                        .filter(e -> e.getId() == edgeId)
                        .findFirst()
                        .orElse(null);

                if (edge != null) {
                    chosenEdge = edge;
                } else {
                    System.out.println("Invalid edge. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        return chosenEdge;
    }

}

