package A2;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a node in the game board.
 */
public class Node {
	/**
	 * Unique identifier for the node.
	 */
	private int id;
	/**
	 * Player who owns a building on this node.
	 */
	private Player owner;
	/**
	 * Type of building on this node (Settlement or City).
	 */
	private BuildingType building;
	/**
	 * Neighbors of this node.
	 */
	private List<Node> neighbors;
	/**
	 * Edges connected to this node.
	 */
	private List<Edge> connectedEdges;

	/**
	 * Constructor for Node.
	 */
	public Node() {
		this.owner = null;
		this.building = null;
		this.neighbors = new ArrayList<>();
		this.connectedEdges = new ArrayList<>();
	}

	/**
	 * Constructor with ID.
	 * @param id The node's ID.
	 */
	public Node(int id) {
		this();
		this.id = id;
	}

	/**
	 * Checks if the node is already occupied by a building.
	 * @return True if occupied, false otherwise.
	 */
	public boolean isOccupied() {
		return owner != null;
	}
    public int getId() {
		return id;
	}

    public Player getOwner() {
		return owner;
	}

    public void setOwner(Player owner) {
		this.owner = owner;
	}

    public BuildingType getBuilding() {
		return building;
	}

    public void setBuilding(BuildingType building) {
		this.building = building;
	}

    public List<Edge> getConnectedEdges() {
		return connectedEdges;
	}

    public List<Node> getNeighbors() {
		return neighbors;
	}
    
    public void addNeighbor(Node node) {
        neighbors.add(node);
    }
    

}