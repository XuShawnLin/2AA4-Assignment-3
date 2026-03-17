package A3;

/**
 * Class representing a building a city command with redo and undo features
 */
public class BuildRoadCommand implements Command {

    private final BuildStructure buildService; //initializing all needed objects for building a road
    private final Player player;
    private final Edge edge;
    private final Board board;
    private boolean done; //holds where the city was built or not

    /**
     * Constructor for BuildRoadCommand
     */
    public BuildRoadCommand(BuildStructure buildService, Player player, Edge edge, Board board) {
        this.buildService = buildService;
        this.player = player;
        this.edge = edge;
        this.board = board;
        this.done = false; //set to false originally since not executed yet
    }

    /**
     * Execute Class
     */
    @Override
    public void execute() {
        done = buildService.buildRoad(player, edge, board); //sets execute to true of the road can be built
    }

    /**
     * Undo Class
     */
    @Override
    public void undo() {
        if (done == false) {
            return; //if it was never done, nothing to undo
        }

        edge.setOwner(null); //restores the properties
        edge.setBuilding(null);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.LUMBER, 1);
        done = false; //set to undone
    }

    /**
     * Class to return status of done variable
     */
    public boolean wasDone() {
        return done; //status of undo/redo
    }
}