package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.util.*;
import com.kuuhaku.util.IO;
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
	private byte renderState = 0b1;
	/*
	0xF
	  └ 000 0111
	         ││└─ will not be rendered
	         │└── path rendered
	         └─── node rendered
	 */

	private NodeType type;
	private int pathColor = -1;

	public Node(Sublevel sublevel, NodeType type) {
		this(sublevel, type, List.of());
	}

	public Node(Sublevel sublevel, List<Node> parents) {
		this(sublevel, NodeType.NONE, parents);

		if (sublevel.getSublevel() == sublevel.getFloor().size() - 1) {
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

	public String getId() {
		return sublevel.getFloor().getFloor() + "-" + sublevel.getSublevel() + "-" + path;
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

	public int travelDistance(Node node) {
		if (node == null || equals(node)) return 0;

		if (!isReturnNode()) {
			for (Node parent : parents) {
				int dist = parent.travelDistance(node) + 1;
				if (dist > 0) return dist;
			}
		}

		return -1;
	}

	public Point getRenderPos() {
		return renderPos;
	}

	public boolean willBeRendered() {
		return Bit32.on(renderState, 0);
	}

	public void setWillBeRendered(boolean will) {
		renderState = (byte) Bit32.set(renderState, 0, will);
	}

	public boolean isPathRendered() {
		return Bit32.on(renderState, 1);
	}

	public void setPathRendered(boolean rendered) {
		renderState = (byte) Bit32.set(renderState, 1, rendered);
	}

	public boolean isNodeRendered() {
		return Bit32.on(renderState, 2);
	}

	public void setNodeRendered(boolean rendered) {
		renderState = (byte) Bit32.set(renderState, 2, rendered);
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

	public boolean isSafeNode() {
		return type == NodeType.BOSS || !sublevel.getFloor().isUnsafeArea();
	}

	public boolean isReturnNode() {
		if (type != NodeType.BOSS) return false;

		for (Node child : children) {
			if (child.getSublevel().getSublevel() == 0) return true;
		}

		return false;
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
			boolean leap = child.depth() - depth() > 1;
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

	public void renderPath(Graphics2D g2d, int width, boolean reachable) {
		Composite comp = g2d.getComposite();
		for (Node child : children) {
			Point to = child.getRenderPos();
			boolean leap = child.depth() - depth() > 1;
			boolean retNode = isReturnNode();
			boolean blocked = this.blocked.contains(child);

			Color color = colorForIndex(child.pathColor);
			if (leap || (child.getParents().size() > 1 && children.size() == 1)) {
				color = colorForIndex(pathColor);
			}

			int strokeWidth = blocked ? 2 : 8;
			if (blocked) {
				color = new Color(255, 0, 0, 128);
			} else if (!reachable) {
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
				color = Color.DARK_GRAY;
			}

			if (leap || retNode) {
				int[] arrX, arrY;

				if (retNode) {
					arrX = new int[]{renderPos.x, width - 20, width - 20, to.x};
					arrY = new int[]{renderPos.y, renderPos.y, to.y, to.y};
				} else {
					Point middle = new Point(0, renderPos.y - (renderPos.y - to.y) / 2);
					if (this.path >= sublevel.size() / 2d) {
						middle.x = Math.max(renderPos.x, to.x);
					} else {
						middle.x = Math.min(renderPos.x, to.x);
					}

					arrX = new int[]{renderPos.x, middle.x, to.x};
					arrY = new int[]{renderPos.y, middle.y, to.y};
				}

				g2d.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(strokeWidth + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1, new float[]{17}, 0));
				g2d.drawPolyline(arrX, arrY, 3);

				if (child.willBeRendered()) {
					g2d.setColor(color);
				} else {
					assert color != null;
					g2d.setPaint(new GradientPaint(renderPos, color, to, Color.BLACK));
				}

				g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1, new float[]{17}, 0));
				g2d.drawPolyline(arrX, arrY, 3);
			} else {
				g2d.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(strokeWidth + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1));
				g2d.drawLine(renderPos.x, renderPos.y, to.x, to.y);

				if (child.willBeRendered()) {
					g2d.setColor(color);
				} else {
					assert color != null;
					g2d.setPaint(new GradientPaint(renderPos, color, to, Color.BLACK));
				}

				g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1));
				g2d.drawLine(renderPos.x, renderPos.y, to.x, to.y);
			}
		}

		g2d.setComposite(comp);
		setPathRendered(true);
	}

	public void renderNode(Graphics2D g2d, Node playerNode, boolean reachable) {
		BufferedImage icon;
		if (!reachable || sublevel.getFloor().areNodesHidden()) {
			icon = new BufferedImage(ICON_PLAIN.getWidth(), ICON_PLAIN.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < icon.getHeight(); y++) {
				for (int x = 0; x < icon.getWidth(); x++) {
					int rgb = ICON_PLAIN.getRGB(x, y);
					int alpha = ((rgb >> 24) & 0xFF) / 2;
					int tone = (rgb & 0xFF) / 3;

					icon.setRGB(x, y, (alpha << 24) | tone << 16 | tone << 8 | tone);
				}
			}
		} else if (equals(playerNode)) {
			icon = ICON_PLAYER;
		} else {
			icon = getIcon();
		}

		g2d.drawImage(icon,
				renderPos.x - Node.NODE_RADIUS / 2, renderPos.y - Node.NODE_RADIUS / 2,
				Node.NODE_RADIUS, Node.NODE_RADIUS,
				null
		);

		setNodeRendered(true);
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
