package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Node {
	public static final int NODE_RADIUS = 64;
	public static final BufferedImage ICON_PLAIN = new Node(null, NodeType.NONE).getIcon();
	public static final BufferedImage ICON_PLAYER = new Node(null, NodeType.PLAYER).getIcon();

	private final Sublevel sublevel;
	private final int path;
	private final int seed;
	private final LinkedHashSet<Node> parents;
	private final LinkedHashSet<Node> children = new LinkedHashSet<>();

	private final Set<String> eventPool = new HashSet<>();
	private final Set<String> enemyPool = new HashSet<>();
	private final Set<Node> blocked = new HashSet<>();
	private final Point renderPos = new Point();
	private boolean renderedPath = false;
	private boolean renderedNode = false;

	private NodeType type;
	private int pathColor = -1;

	public Node(Sublevel sublevel, NodeType type) {
		this(sublevel, type, List.of());
	}

	public Node(Sublevel sublevel, List<Node> parents) {
		this(sublevel, NodeType.NONE, parents);

		if (getParents().stream().anyMatch(p -> p.getType() == NodeType.BOSS)) {
			this.type = NodeType.REST;
		} else if (sublevel.getSublevel() == sublevel.getFloor().size() - 1) {
			this.type = NodeType.BOSS;
		}
	}

	public Node(Sublevel sublevel, NodeType type, Collection<Node> parents) {
		this.sublevel = sublevel;
		this.path = sublevel == null ? 0 : sublevel.size();
		this.type = type;
		this.seed = sublevel == null ? 0 : Utils.generateSeed(DigestUtils.getMd5Digest(),
				sublevel.getFloor().getSeed(),
				sublevel.getSublevel(),
				path
		);

		this.parents = new LinkedHashSet<>(parents);
		for (Node parent : parents) {
			parent.children.add(this);
		}
	}

	public Sublevel getSublevel() {
		return sublevel;
	}

	public int getPath() {
		return path;
	}

	public int getSeed() {
		return seed;
	}

	public Set<String> getEventPool() {
		return eventPool;
	}

	public Set<String> getEnemyPool() {
		return enemyPool;
	}

	public void addParents(Node... nodes) {
		for (Node node : nodes) {
			parents.add(node);
			node.children.add(this);
		}
	}

	public LinkedHashSet<Node> getParents() {
		return parents;
	}

	public void addChildren(Node... nodes) {
		for (Node node : nodes) {
			children.add(node);
			node.parents.add(this);
		}
	}

	public LinkedHashSet<Node> getChildren() {
		return children;
	}

	public Set<Node> getBlocked() {
		return blocked;
	}

	public boolean canReach(Node node) {
		if (node == null || equals(node)) return true;

		for (Node parent : parents) {
			if (parent.canReach(node)) return true;
		}

		return false;
	}

	public Point getRenderPos() {
		return renderPos;
	}

	public boolean isPathRendered() {
		return renderedPath;
	}

	public void setPathRendered(boolean rendered) {
		this.renderedPath = rendered;
	}

	public boolean isNodeRendered() {
		return renderedNode;
	}

	public void setNodeRendered(boolean rendered) {
		this.renderedNode = rendered;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public int getNodeLevel() {
		return type == NodeType.DANGER ? 5 : 0;
	}

	public BufferedImage getIcon() {
		return IO.getResourceAsImage("dunhun/icons/node_" + getType().name().toLowerCase() + ".png");
	}

	public int depth() {
		return sublevel.depth();
	}

	public boolean isOccluded(int width, int height) {
		return !Calc.between(renderPos.x, 0, width) || !Calc.between(renderPos.y, 0, height);
	}

	private Color colorForIndex(int index) {
		if (index < 0) return null;
		return Color.getHSBColor(1f / (Sublevel.MAX_NODES + 1) * index, 0.5f, 0.5f);
	}

	public void calcColor() {
		if (pathColor == -1) {
			pathColor = path;
		}

		boolean split = children.size() > 1;
		for (Node child : children) {
			boolean leap = child.getSublevel().getSublevel() - sublevel.getSublevel() > 1;
			if (leap || child.pathColor != -1) continue;

			int colorAdd = split ? child.path - this.path : 0;
			child.pathColor = pathColor + colorAdd;
		}
	}

	public String getPathVerb(Node to) {
		Point fromPos = getRenderPos();
		Point toPos = to.getRenderPos();

		if (fromPos.equals(toPos)) return "centercenter";

		Point dir = new Point(toPos.x - fromPos.x, toPos.y - fromPos.y);
		double len = Math.max(Math.abs(dir.x), Math.abs(dir.y));
		Point2D.Double normal = new Point2D.Double(dir.x / len, dir.y / len);

		String icon;
		if (normal.x <= -0.25) {
			icon = "left";
		} else if (normal.x >= 0.25) {
			icon = "right";
		} else {
			icon = "center";
		}

		if (normal.y <= -0.25) {
			icon += "top";
		} else if (normal.y >= 0.25) {
			icon += "bottom";
		} else {
			icon += "center";
		}

		return icon;
	}

	public String getPathIcon(List<Node> children) {
		int idx = children.indexOf(this) + 1;
		int sibls = children.size();

		if (sibls % 2 == 1 && idx == sibls / 2 + 1) return "center";
		else if (idx <= sibls / 2) {
			if (idx == 1 && sibls > 3) return "leftmost";
			return "left";
		} else {
			if (idx == sibls && sibls > 3) return "rightmost";
			return "right";
		}
	}

	public void renderPath(Graphics2D g2d, Node playerNode) {
		boolean reachable = canReach(playerNode);

		for (Node child : children) {
			Point to = child.getRenderPos();
			boolean leap = child.getSublevel().getSublevel() - sublevel.getSublevel() > 1;
			boolean blocked = this.blocked.contains(child);

			Color color = colorForIndex(child.pathColor);
			if (leap || (child.getParents().size() > 1 && children.size() == 1)) {
				color = colorForIndex(pathColor);
			}

			int strokeWidth = blocked ? 2 : 8;
			if (blocked) {
				color = new Color(255, 0, 0, 128);
			} else {
				color = reachable ? color : Color.DARK_GRAY;
			}

			if (leap) {
				Point middle = new Point(0, renderPos.y - (renderPos.y - to.y) / 2);
				if (this.path >= sublevel.size() / 2d) {
					middle.x = Math.max(renderPos.x, to.x);
				} else {
					middle.x = Math.min(renderPos.x, to.x);
				}

				int[] arrX = new int[]{renderPos.x, middle.x, to.x};
				int[] arrY = new int[]{renderPos.y, middle.y, to.y};

				g2d.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(strokeWidth + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1, new float[]{17}, 0));
				g2d.drawPolyline(arrX, arrY, 3);

				g2d.setColor(color);
				g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1, new float[]{17}, 0));
				g2d.drawPolyline(arrX, arrY, 3);
			} else {
				g2d.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(strokeWidth + 3));
				g2d.drawLine(renderPos.x, renderPos.y, to.x, to.y);

				g2d.setColor(color);
				g2d.setStroke(new BasicStroke(strokeWidth));
				g2d.drawLine(renderPos.x, renderPos.y, to.x, to.y);
			}
		}

		renderedPath = true;
	}

	public void renderNode(Graphics2D g2d, Node playerNode) {
		boolean reachable = canReach(playerNode);

		BufferedImage nodeIcon = getIcon();
		if (!reachable) {
			for (int y = 0; y < nodeIcon.getHeight(); y++) {
				for (int x = 0; x < nodeIcon.getWidth(); x++) {
					int rgb = ICON_PLAIN.getRGB(x, y);
					int alpha = ((rgb >> 24) & 0xFF) / 2;
					int tone = (rgb & 0xFF) / 3;

					nodeIcon.setRGB(x, y, (alpha << 24) | tone << 16 | tone << 8 | tone);
				}
			}
		}

		BufferedImage icon = nodeIcon;
		if (sublevel.getFloor().areNodesHidden()) {
			icon = ICON_PLAIN;
		} else if (equals(playerNode)) {
			icon = ICON_PLAYER;
		}

		g2d.drawImage(icon,
				renderPos.x - Node.NODE_RADIUS / 2, renderPos.y - Node.NODE_RADIUS / 2,
				Node.NODE_RADIUS, Node.NODE_RADIUS,
				null
		);

		renderedNode = true;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return path == node.path && Objects.equals(sublevel, node.sublevel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sublevel, path);
	}
}
