package A2;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an edge between nodes where roads can be built.
 */
public class Edge {
	/**
	 * Unique identifier for the edge.
	 */
	private int id;

	/**
	 * Player who owns a road on this edge.
	 */
	private Player owner;

	/**
	 * Type of building on this edge (Road).
	 */
	private BuildingType building;

	/**
	 * Nodes connected by this edge.
	 */
	private List<Node> connectedNodes;

	/**
	 * Constructor for Edge.
	 */
	public Edge() {
		this.owner = null;
		this.building = null;
		this.connectedNodes = new ArrayList<>();
	}

	/**
	 * Constructor with ID.
	 * @param id The edge's ID.
	 */
	public Edge(int id) {
		this();
		this.id = id;
	}

	/**
	 * Gets the edge's unique ID.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Checks if the edge is already occupied by a road.
	 * @return True if occupied, false otherwise.
	 */
	public boolean isOccupied() {
		return owner != null;
	}

	public Player getOwner() {
		return owner;
	}

	public void setOwner(Player owner) {
		this.owner = owner;
	}

	public void setBuilding(BuildingType building) {
		this.building = building;
	}

	public BuildingType getBuilding() {
		return building;
	}

	public List<Node> getConnectedNodes() {
		return connectedNodes;
	}

	public void addConnectedNode(Node node) {
		connectedNodes.add(node);
	}
}