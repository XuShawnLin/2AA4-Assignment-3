package A3;

import java.util.EnumMap;

/**
 * Class representing the bank in the game.
 */
public class Bank {
	private EnumMap<ResourceType, Integer> resourceList;

	/**
	 * Constructor for Bank class.
	 */
	public Bank() {
		this.resourceList = new EnumMap<>(ResourceType.class);
		for (ResourceType type : ResourceType.values()) {
			resourceList.put(type, 19); // Standard Catan bank has 19 of each
		}
	}

	/**
	 * Checks if the bank has a specified amount of a resource.
	 * @return True if the bank has the resource, false otherwise.
	 * @param type The type of resource to check.
	 * @param amount The amount of the resource to check.
	 */
	public boolean hasResource(ResourceType type, int amount) {
		return resourceList.getOrDefault(type, 0) >= amount;
	}

	/**
	 * Gives a specified amount of a resource to the bank.
	 * @return True if the resource was successfully given, false otherwise.
	 * @param type The type of resource to give.
	 * @param amount The amount of the resource to give.
	 */
	public boolean giveResource(ResourceType type, int amount) {
		if (hasResource(type, amount)) {
			resourceList.put(type, resourceList.get(type) - amount);
			return true;
		}
		return false;
	}

	/**
	 * Receives a specified amount of a resource from the bank.
	 * @return True if the resource was successfully received, false otherwise.
	 * @param type The type of resource to receive.
	 * @param amount The amount of the resource to receive.
	 */
	public boolean receiveResource(ResourceType type, int amount) {
		resourceList.put(type, resourceList.getOrDefault(type, 0) + amount);
		return true;
	}
	
	public int getResourceCount(ResourceType type) {
	    return resourceList.get(type);
	}
}
