package A3;

/**
 * Handles resource distribution for a given dice roll (SRP).
 * Logic mirrors the original Game.distributeResources.
 */
public class ResourceDistributor {

    public void distribute(int roll, Board board, Player[] players, Bank bank) {
        if (board == null || bank == null) return;
        for (HexTile tile : board.getTiles()) {
            Integer token = tile.getTokenNumber();
            if (token == null || token.intValue() != roll) continue;

            ResourceType resource = tile.getResource();
            if (resource == null) continue; // desert / no resource

            for (Node node : tile.getNodes()) {
                if (node.isOccupied() && bank.giveResource(resource, 1)) {
                    node.getOwner().addResource(resource, 1);
                }
            }
        }
    }
}
