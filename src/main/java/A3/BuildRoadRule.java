package A3;

public class BuildRoadRule implements Rule {

    @Override
    public double evaluate(Player p, Board board) {
        for (Edge e : board.getEdges()) {
            if (!e.isOccupied() && board.isValidRoad(e, p)) {
                return 0.8; //value associated with building something
            }
        }
        return 0;
    }

    @Override
    public boolean apply(Player p, Board board, BuildStructure buildService, int round) {
        for (Edge e : board.getEdges()) {
            if (!e.isOccupied() && board.isValidRoad(e, p)
                    && buildService.buildRoad(p, e, board)) {
                return true;
            }
        }
        return false;
    }
}
