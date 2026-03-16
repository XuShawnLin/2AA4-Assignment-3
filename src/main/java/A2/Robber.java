package A2;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the robber behavior:
 */
public class Robber {

    private HexTile currentTile;
    private final SecureRandom randomNum; //using secure random as its a safer random number generator (suggested by Sonar Qube)

    public Robber() {
        this(new SecureRandom());
    }

    public Robber(SecureRandom randomNum) {
        this.randomNum = randomNum;
        this.currentTile = null; //will be set when moved to a tile
    }

    public HexTile getCurrentTile() {
        return currentTile;
    }

    /**
     * Called when a 7 is rolled.
     * @param board the Board
     * @param roller the player who rolled the 7
     */
    public void rollSeven(Board board, Player[] players, Player roller) {
        if (players != null) { //discard half for random any player with more than 7 total resources
            for (Player player : players) {
                removeHalf(player);
            }
        }
        moveTile(board); //mode robber to random tile
        
        Player victim = getVictim(currentTile, roller); //choosing victim adajcent to robber tile
        if (victim == null) return;

        ResourceType stolen = stealRandomResource(victim); //steal random resource from victim
        if (stolen != null) {
            roller.addResource(stolen, 1);
        }
    }

    /**
     * If player has more than 7 resources, discard half randomly
     */
    private void removeHalf(Player player) {
        int total = player.getTotalResources();
        if (total <= 7){
            return;
        }

        int discardHalf = total / 2; // discard half
        while (discardHalf > 0) {
            ResourceType chooseSource = randomOwnedResource(player);
            if (chooseSource == null) break; //shouldnt happen unless map is inconsistent
            boolean removed = player.removeResource(chooseSource, 1);
            if (removed) {
                discardHalf--;
            }
        }
    }

    /**
     * Moves robber to a random tile
     */
    private void moveTile(Board board) {
        if (board == null || board.getTiles() == null || board.getTiles().isEmpty()) {
            currentTile = null;
            return;
        }
        int index = randomNum.nextInt(board.getTiles().size());
        currentTile = board.getTiles().get(index);
    }

    /**
     * Victim must own a node on the robber tile, and is not the roller.
     * Picks randomly from players.
     */
    private Player getVictim(HexTile tile, Player roller) {
        if (tile == null) {
            return null;
        }

        List<Player> playersLeft = new ArrayList<>();
        for (Node node : tile.getNodes()) {
            if (node != null && node.isOccupied() && node.getOwner() != null) {
                Player owner = node.getOwner();
                if (owner != roller && !playersLeft.contains(owner) && owner.getTotalResources() > 0) {  //only consider owners who actually have something to steal
                    playersLeft.add(owner);
                }
            }
        }

        if (playersLeft.isEmpty()) {
            return null;
        }
        return playersLeft.get(randomNum.nextInt(playersLeft.size()));
    }

    /**
     * Removes and returns a random resource type from the victim (or null if none).
     */
    private ResourceType stealRandomResource(Player victim) {
        ResourceType pick = randomOwnedResource(victim);
        if (pick == null) {
            return null;
        }

        boolean removed = victim.removeResource(pick, 1);
        if (!removed) {
            return null;
        }
        return pick;
    }

    /**
     * Choose a random ResourceType that the player currently has more than 0 of
     */
    private ResourceType randomOwnedResource(Player player) {
        Map<ResourceType, Integer> resource = player.getCurrentResources();

        List<ResourceType> available = new ArrayList<>();
        for (ResourceType type : ResourceType.values()) {
            Integer amount = resource.get(type);
            if (amount != null && amount > 0) {
                available.add(type);
            }
        }

        if (available.isEmpty()) {
            return null;
        }
        return available.get(randomNum.nextInt(available.size()));
    }
}