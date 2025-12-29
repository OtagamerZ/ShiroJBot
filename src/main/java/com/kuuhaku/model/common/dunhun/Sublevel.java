package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

	public Node newNode(NodeType type) {
		return newNode(type, List.of());
	}

	public Node newNode(NodeType type, List<Node> parents) {
		Node node = new Node(this, type, parents);
		nodes.add(node);
		return node;
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
				if (depth() - parent.depth() > 1) {
					return true;
				}
			}
		}

		return false;
	}

	public void placeNodes(int x, int y) {
		Map<Boolean, List<Node>> groups = nodes.stream().collect(Collectors.groupingBy(Node::isNodeOffset));
		List<Node> normal = groups.get(false);
		List<Node> offsets = groups.get(true);

		int space = Node.NODE_RADIUS + Node.NODE_SPACING;
		for (int i = 0; i < normal.size(); i++) {
			Node node = normal.get(i);
			int offset = space * i - (normal.size() - 1) * space / 2;
			int posX = x + offset;

			node.getRenderPos().move(posX, y);
			node.setPathRendered(false);
			node.setNodeRendered(false);
		}

		for (int i = 0; i < offsets.size(); i++) {
			Node node = offsets.get(i);
			int offset = space * (normal.size() - 1) - (offsets.size() - 1) * space / 2;
			int posX = x + offset + space * i;

			node.getRenderPos().move(posX, y);
			node.setPathRendered(false);
			node.setNodeRendered(false);
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
