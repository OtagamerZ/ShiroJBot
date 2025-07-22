package com.kuuhaku.model.common.dunhun;

import java.util.ArrayList;
import java.util.List;

public class Floor {
	private final List<Node> nodes = new ArrayList<>();
	private int nodeSpacing = 40;

	public List<Node> getNodes() {
		return nodes;
	}

	public int getNodeSpacing() {
		return nodeSpacing;
	}

	public void prepare(int width, int y) {
		int waypointCount = nodes.size();

		for (int i = 0; i < waypointCount; i++) {
			Node node = nodes.get(i);
			int space = Node.NODE_RADIUS + nodeSpacing;
			int offset = space * i - (waypointCount - 1) * space / 2;
			int x = width / 2 + offset;
			node.getPos().setLocation(x, y);
		}
	}
}
