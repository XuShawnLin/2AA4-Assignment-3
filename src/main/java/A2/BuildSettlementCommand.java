package A2;

/**
 * Class representing a building a settlement command with redo and undo features
 */
public class BuildSettlementCommand implements Command {

    private final BuildStructure buildService; //initializing all needed objects for building a road
    private final Player player;
    private final Node node;
    private final Board board;
    private boolean done; //holds where the settlement was built or not

    /**
     * Constructor for BuildSettlemntCommand
     */
    public BuildSettlementCommand(BuildStructure buildService, Player player, Node node, Board board) {
        this.buildService = buildService;
        this.player = player;
        this.node = node;
        this.board = board;
        this.done = false;
    }

    /**
     * Execute Class
     */
    @Override
    public void execute() {
        done = buildService.buildSettlement(player, node, board); //sets execute to true of the road can be built
    }

    /**
     * Undo Class
     */
    @Override
    public void undo() {
        if (done == false) {
            return; //if it was never done, nothing to undo
        }

        node.setOwner(null); //restores the properties
        node.setBuilding(null);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.LUMBER, 1);
        player.addResource(ResourceType.WOOL, 1);
        player.addResource(ResourceType.GRAIN, 1);
        player.addVictoryPoints(-1);
        done = false; //set to undone
    }

    /**
     * Class to return status of done variable
     */
    public boolean wasDone() {
        return done; //status of undo/redo
    }
}