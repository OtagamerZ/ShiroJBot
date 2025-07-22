package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.EventType;
import com.kuuhaku.util.IO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Node {
	public static final int NODE_RADIUS = 64;

	private final EventType type;
	private final List<Node> parents;
	private final Point pos = new Point();
	private int depth;

	public Node(EventType type, Node... parents) {
		this.type = type;
		this.parents = List.of(parents);
	}

	public EventType getType() {
		return type;
	}

	public Point getPos() {
		return pos;
	}

	public List<Node> getParents() {
		return parents;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public BufferedImage getIcon() {
		return IO.getResourceAsImage("dunhun/icons/node_" + type.name().toLowerCase() + ".png");
	}
}
