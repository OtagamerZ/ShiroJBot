package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Sublevel {
	public static final int MAX_NODES = 7;

	private final Floor floor;
	private final int sublevel;
	private final List<Node> nodes = new ArrayList<>(MAX_NODES);

	public Sublevel(Floor floor, int sublevel) {
		this.floor = floor;
		this.sublevel = sublevel;
	}

	public Floor getFloor() {
		return floor;
	}

	public Node getNode(int index) {
		return nodes.get(index);
	}

	public Node getAt(int index) {
		return getNode(index);
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public void newNode(NodeType type, List<Node> parents, Consumer<Node> generator) {
		Node node = new Node(this, type, parents);
		generator.accept(node);
		nodes.add(node);
	}

	public void addNode(Node... parents) {
		addNode(List.of(parents));
	}

	public void addNode(List<Node> parents) {
		nodes.add(new Node(this, parents));
	}

	public void addNode(NodeType type, Node... parents) {
		addNode(type, List.of(parents));
	}

	public void addNode(NodeType type, List<Node> parents) {
		nodes.add(new Node(this, type, parents));
	}

	public int getSublevel() {
		return sublevel;
	}

	public int size() {
		return nodes.size();
	}

	public int depth() {
		return floor.depth() + sublevel + 1;
	}

	public boolean hasLeapNode() {
		for (Node node : nodes) {
			for (Node parent : node.getParents()) {
				if (sublevel - parent.getSublevel().getSublevel() > 1) {
					return true;
				}
			}
		}

		return false;
	}

	public void placeNodes(int width, int y) {
		int spacing = 40;
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			int space = Node.NODE_RADIUS + spacing;
			int offset = space * i - (nodes.size() - 1) * space / 2;
			int x = width / 2 + offset;

			node.getRenderPos().move(x, y);
			node.setRendered(false);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Sublevel sublevel1 = (Sublevel) o;
		return sublevel == sublevel1.sublevel && Objects.equals(floor, sublevel1.floor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(floor, sublevel);
	}
}
