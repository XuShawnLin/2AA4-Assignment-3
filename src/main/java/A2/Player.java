package A2;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
/**
 * Class representing a player in the game.
 */
public class Player {
    private int victoryPoint;
    private List<BuildingType> piecesOwned;
    private EnumMap<ResourceType, Integer> resourcedOwned;
    private String name;

	/**
	 * Constructor for Player class if no name.
	 */
 public Player() {
        this.victoryPoint = 2;
        this.piecesOwned = new ArrayList<>();
        this.resourcedOwned = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            resourcedOwned.put(type, 0);
        }
        this.name = "Unknown";
    }

	/**
	 * Constructor for Player class, if they have a name.
	 */
	public Player(String name) {
		this();
		this.name = name;
	}

	/**
	 * Returns name of player.
	 */
	public String getName() {
		return name;
	}
 /**
  * Retrieves the current resources owned by the player.
  */
 public Map<ResourceType, Integer> getCurrentResources() {
     return resourcedOwned;
 }

	/**
	 * Retrieves the current victory points of the player.
	 * @return The current victory points.
	 */
	public int getVictoryPoints() {
		return victoryPoint;
	}

	/**
	 * Add a victory point
	 */
	public void addVictoryPoints(int points) {
		this.victoryPoint += points;
	}
	/**
	 * Add resources to player
	 */
	public void addResource(ResourceType type, int amount) {
		resourcedOwned.put(type, resourcedOwned.get(type) + amount);
	}

	/**
	 * Removes resources when exchanging for VP or building
	 */
	public boolean removeResource(ResourceType type, int amount) {
		int current = resourcedOwned.get(type);
		if (current >= amount) {
			resourcedOwned.put(type, current - amount);
			return true;
		}
		return false;
	}

	public int getTotalResources() {
		int total = 0;
		for (int count : resourcedOwned.values()) {
			total += count;
		}
		return total;
	}

	public void build() {
		// Implementation for building
	}

	public void takeTurn(GameMaster gameMaster, int round) {

        int roll = gameMaster.rollDice();
        System.out.println(round + " / " + name + ": rolled a " + roll);

        if (roll == 7) {
            System.out.println(round + " / " + name + ": robber activated");
        } else {
            gameMaster.distributeResources(roll);
        }

        System.out.println(round + " / " + name + ": ended turn");
    }
}
