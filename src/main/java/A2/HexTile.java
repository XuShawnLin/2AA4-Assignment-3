package A2;

import java.util.ArrayList;
import java.util.List;

/**
 * HexTile class representing a hexagonal tile on the Catan board.
 */
public class HexTile {
	/**
	 * Unique identifier for the tile.
	 */
	private int id;
	/**
	 * Type of resource this tile produces.
	 */
	private ResourceType resource;
	/**
	 * List of nodes (corners) surrounding this tile.
	 */
	private List<Node> nodes;
	/**
	 * List of edges (borders) surrounding this tile.
	 */
	private List<Edge> edges;
	/**
	 * Number token associated with this tile.
	 */
	private Integer tokenNumber; // null if DESERT / no token

	/**
	 * Constructor for HexTile.
	 * @param resource The type of resource this tile produces.
	 * @param token The token number for resource production.
	 */
	public HexTile(ResourceType resource, Integer token) {
		this.resource = resource;
		this.tokenNumber = token;
		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();
	}

	/**
	 * Constructor with ID, ResourceType, and Token (int).
	 * @param id Unique identifier.
	 * @param resource Type of resource.
	 * @param token Token value.
	 */
	public HexTile(int id, ResourceType resource, int token) {
		this.id = id;
		this.resource = resource;
		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.tokenNumber = (token == 0 ? null : Integer.valueOf(token));
	}
	public Integer getTokenNumber() {
		return tokenNumber;
	}

	public ResourceType getResource() {
		return resource;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * Adds a node to this tile.
	 * @param node The node to add.
	 */
	public void addNode(Node node) {
		this.nodes.add(node);
	}

	/**
	 * Distributes resources to players with buildings on this tile's nodes.
	 */
	public void resourceProduction() {
		if (resource == null) return; // Desert or no resource
		for (Node node : nodes) {
			if (node.isOccupied()) {
				int amount = (node.getBuilding() == BuildingType.CITY) ? 2 : 1;
				node.getOwner().addResource(resource, amount);
			}
		}
	}

	public void setTokenNumber(Integer tokenNumber) {
		this.tokenNumber = tokenNumber;
	}

	public void setResource(ResourceType resource) {
		this.resource = resource;
	}


	public List<Edge> getEdges() {
		return edges;
	}
}