package A3;

public class BuildValidator {

    public boolean canBuildRoad(Player player, Edge edge, Board board) {
        if (player == null || edge == null) return false;
        if (edge.isOccupied()) return false;
        if (board != null) return board.isValidRoad(edge, player);
        return true;
    }

    public boolean canBuildSettlement(Player player, Node node, Board board, boolean initialPlacement) {
        if (player == null || node == null) return false;
        if (node.isOccupied()) return false; // node must be free

        // Distance rule
        for (Node neighbor : node.getNeighbors()) {
            if (neighbor.isOccupied()) return false;
        }

        if (initialPlacement) return true; // ignore connectivity

        return board != null && board.isValidSettlement(node, player, false);
    }

    public boolean canBuildCity(Player player, Node node) {
        if (player == null || node == null) return false;
        return node.getOwner() == player && node.getBuilding() == BuildingType.SETTLEMENT;
    }
}
