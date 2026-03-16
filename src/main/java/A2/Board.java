package A2;

import java.util.ArrayList;
import java.util.List;

public class Board {
	private List<HexTile> tiles;
	private List<Node> nodes;
	private List<Edge> edges;

	public Board() {
		this.tiles = new ArrayList<>();
		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();
		initializeBoard();
	}

	private void initializeBoard() {
		for (int i = 0; i < 19; i++) tiles.add(new HexTile(i, null, 0));
		for (int i = 0; i < 54; i++) nodes.add(new Node(i));
	}

	public List<HexTile> getTiles() { return tiles; }
	public List<Node> getNodes() { return nodes; }
	public List<Edge> getEdges() { return edges; }

	public void addTile(HexTile tile) { tiles.add(tile); }
	public void addNode(Node node) { nodes.add(node); }
	public void addEdge(Edge edge) { edges.add(edge); }

	// ---------------- Fixed method ----------------
	public boolean isValidSettlement(Node n, Player p, boolean ignoreRoads) {
		if (n.isOccupied()) return false;

		// Distance rule
		for (Node neighbor : n.getNeighbors()) {
			if (neighbor.isOccupied()) return false;
		}

		if (ignoreRoads) return true; // initial placement ignores connectivity

		// Connectivity: must be connected to player's road
		for (Edge edge : n.getConnectedEdges()) {
			if (edge.getOwner() == p) return true;
		}

		return false; // Not connected
	}

	public boolean isValidRoad(Edge e, Player p) {
		if (e.isOccupied()) return false;

		for (Node node : e.getConnectedNodes()) {
			if (node.getOwner() == p) return true;
			for (Edge adj : node.getConnectedEdges()) {
				if (adj != e && adj.getOwner() == p) return true;
			}
		}

		return false;
	}
}