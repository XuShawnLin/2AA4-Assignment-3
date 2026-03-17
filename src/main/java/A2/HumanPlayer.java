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
    private final CommandManager commandManager; //Added command manager method for undo/redo

    public HumanPlayer(String name) {
        super(name);
        this.scanner = new Scanner(System.in);
        this.commandManager = new CommandManager(); //Initalizating with new command manager
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
            LOGGER.info("Command (Roll, Build, Undo, Redo, List, Go): "); //Updated to include undo/redo
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

                case "undo": //Added new undo case for player move and for logger
                    if (commandManager.undo()) {
                        LOGGER.info("Undo successful.");
                    } else {
                        LOGGER.info("Nothing to undo.");
                    }
                    break;

                case "redo": //Added new redo case for player move and for logger
                    if (commandManager.redo()) {
                        LOGGER.info("Redo successful.");
                    } else {
                        LOGGER.info("Nothing to redo.");
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

                Node settlementNode = chooseSettlementNode(board); //changed to follow rules of choosing settlement node
                if (settlementNode == null) return; // back option

                BuildSettlementCommand buildSettlementCommand = new BuildSettlementCommand(buildService, this, settlementNode, board); //created the needed command object here instead of direct build logic

                commandManager.executeCommand(buildSettlementCommand); //executes command and stores in history so undo/redo can be used later

                if (buildSettlementCommand.wasDone()) { //if command done then build the settlement
                    LOGGER.log(Level.INFO, "{0}: built a settlement on node {1}",
                            new Object[]{getName(), settlementNode.getId()});
                } else { //error if cant be built
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

                Node cityNode = chooseCityNode(board); //changed to follow rules of choosing city node
                if (cityNode == null) return; // back option

                BuildCityCommand buildCityCommand = new BuildCityCommand(buildService, this, cityNode, board); //created the needed command object here instead of direct build logic

                commandManager.executeCommand(buildCityCommand); //executes command and stores in history so undo/redo can be used later

                if (buildCityCommand.wasDone()) { //if command done then build the city
                    LOGGER.log(Level.INFO, "{0}: built a city on node {1}",
                            new Object[]{getName(), cityNode.getId()});
                } else { //error if cant be built
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

                BuildRoadCommand buildRoadCommand = new BuildRoadCommand(buildService, this, edge, board); //created the needed command object here instead of direct build logic

                commandManager.executeCommand(buildRoadCommand); //executes command and stores in history so undo/redo can be used later

                if (buildRoadCommand.wasDone()) { //if command done then build the road
                    LOGGER.log(Level.INFO, "{0}: built a road on edge {1}",
                            new Object[]{getName(), edge.getId()});
                } else { //error if cant be built
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

    private Node chooseSettlementNode(Board board) { //Builds off of the original chooseBuildNode method but specifally for settlements
        Node chosenNode = null;

        while (chosenNode == null) { //keep asking until a valid node is selected or the player cancels
            System.out.print("Enter node id or 'back': ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("back")) { //lets player cancel the build action
                return null;
            }

            try {
                int nodeId = Integer.parseInt(input); //convert user input to a node id
                Node node = board.getNodes().stream() //find matching node
                        .filter(n -> n.getId() == nodeId)
                        .findFirst()
                        .orElse(null);

                if (node != null && board.isValidSettlement(node, this, false)) { //check if  node exists and if a settlement can be built there
                    chosenNode = node;
                } else {
                    System.out.println("Cannot build settlement there.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }

        return chosenNode;
    }

    private Node chooseCityNode(Board board) { //Builds off of the original chooseBuildNode method but specifally for cities
        Node chosenNode = null;

        while (chosenNode == null) { //keep asking until a valid node is selected or the player cancels
            System.out.print("Enter settlement node id to upgrade or 'back': ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("back")) { //lets player cancel the build action
                return null;
            }

            try {
                int nodeId = Integer.parseInt(input); //convert user input to a node id
                Node node = board.getNodes().stream() //find matching node
                        .filter(n -> n.getId() == nodeId)
                        .findFirst()
                        .orElse(null);

                if (node != null && //check node with player and contains settlement
                        node.getOwner() == this &&
                        node.getBuilding() == BuildingType.SETTLEMENT) {

                    chosenNode = node;

                } else {
                    System.out.println("You must choose one of your settlements.");
                }

            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }

        return chosenNode;
    }
}

