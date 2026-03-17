package A3;

/**
 * Class representing a building a city command with redo and undo features
 */
public class BuildCityCommand implements Command {

    private final BuildStructure buildService; //initializing all needed objects for building a city
    private final Player player;
    private final Node node;
    private final Board board;
    private boolean done; //holds where the city was built or not

    /**
     * Constructor for BuildCityCommand
     */
    public BuildCityCommand(BuildStructure buildService, Player player, Node node, Board board) {
        this.buildService = buildService;
        this.player = player;
        this.node = node;
        this.board = board;
        this.done = false; //set to false originally since not executed yet
    }

    /**
     * Execute Class
     */
    @Override
    public void execute() {
        done = buildService.buildCity(player, node, board); //sets execute to true of the city can be built
    }

    /**
     * Undo Class
     */
    @Override
    public void undo() {
        if (done == false) {
            return; //if it was never done, nothing to undo
        }

        node.setBuilding(BuildingType.SETTLEMENT); //restores the properties
        player.addResource(ResourceType.GRAIN, 2);
        player.addResource(ResourceType.ORE, 3);
        player.addVictoryPoints(-1); //reduce victory points
        done = false; //set to undone
    }

    /**
     * Class to return status of done variable
     */
    public boolean wasDone() {
        return done; //status of undo/redo
    }
}