package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.util.WobbleStroke;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AreaMap {
	public static final int RENDER_NODES = 10;
	public static final int RENDER_FLOORS = 1;
	private static final Point ZERO = new Point();

	private final Consumer<AreaMap> generator;
	private final TreeMap<Integer, Floor> floors = new TreeMap<>();
	private final AtomicInteger renderFloor = new AtomicInteger(0);
	private final AtomicInteger renderSublevel = new AtomicInteger(0);
	private final PlayerPos playerPos = new PlayerPos();
	private CompletableFuture<Void> generated;

	public AreaMap(Consumer<AreaMap> generator) {
		this(0, generator);
	}

	public AreaMap(int floor, Consumer<AreaMap> generator) {
		this.renderFloor.set(floor);
		this.generator = generator;
	}

	public List<Floor> getFloors() {
		return List.copyOf(floors.values());
	}

	public void addFloor(Floor floor) {
		floors.put(floor.getFloor(), floor);
	}

	public Floor getFloor(int depth) {
		return floors.get(depth);
	}

	public Node getPlayerNode() {
		if (!floors.containsKey(playerPos.getFloor())) return null;

		return getFloor(playerPos.getFloor())
				.getSublevel(playerPos.getSublevel())
				.getNode(playerPos.getPath());
	}

	public int getRenderFloor() {
		return renderFloor.get();
	}

	public int getRenderSublevel() {
		return renderSublevel.get();
	}

	public void pan(int dy) {
		int dir = (int) Math.signum(dy);
		for (int i = Math.abs(dy); i > 0; i--) {
			int sublevel = renderSublevel.addAndGet(dir);
			if (sublevel < 0) {
				int size = getFloor(renderFloor.decrementAndGet()).size();
				renderSublevel.set(size - 1);
			} else {
				int size = getFloor(renderFloor.get()).size();

				if (sublevel >= size) {
					renderFloor.incrementAndGet();
					renderSublevel.set(0);
				}
			}
		}
	}

	public PlayerPos getPlayerPos() {
		return playerPos;
	}

	public void generate() {
		generated = CompletableFuture.runAsync(() -> {
			floors.clear();
			generator.accept(this);
		});
	}

	public BufferedImage render(int width, int height) {
		if (generated != null) {
			generated.join();
		}

		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int floorCount = floors.size();
		if (floorCount == 0) return bi;

		int sliceHeight = height / RENDER_NODES;
		{
			int y = -sliceHeight * renderSublevel.get();
			if (renderFloor.get() == 0) {
				y += 25;
			}

			for (Floor fl : floors.values()) {
				if (fl.getFloor() < renderFloor.get()) continue;

				boolean label = fl.getFloor() > 0;
				if (label) {
					g2d.setColor(Color.DARK_GRAY);
					g2d.drawLine(0, y, width, y);

					g2d.setFont(new Font("Arial", Font.BOLD, 25));
					g2d.drawString("Floor " + fl.getFloor() + " ", 5, y + g2d.getFontMetrics().getHeight());
					y += 50;
				} else {
					g2d.setColor(new Color(138, 168, 179));
					g2d.fillRect(0, y - 25, width, sliceHeight + 50);
					y += 25;
				}

				if (fl.getFloor() == renderFloor.get()) {
					Floor prev = floors.get(fl.getFloor() - 1);
					if (prev != null && prev.getFloor() >= 0) {
						Sublevel last = prev.getSublevels().get(prev.size() - 1);
						last.placeNodes(width, y - sliceHeight - 50);
					}
				}

				for (Sublevel sub : fl.getSublevels()) {
					sub.placeNodes(width, y + ((fl.getFloor() == 0 ? 25 : 0)));
					y += sliceHeight;
				}
			}
		}

		Random bgRng = new Random(floors.hashCode());
		Node playerNode = getPlayerNode();

		List<Floor> floors = List.copyOf(this.floors.values());
		Map<Floor, List<Node>> nodes = new TreeMap<>(Comparator.comparingInt(Floor::getFloor));
		for (int i = 0; i < 2; i++) {
			for (Floor fl : floors) {
				List<Node> nds = nodes.computeIfAbsent(fl, f -> fl.getNodes());
				if (i == 0) {
					for (Node node : nds) {
						if (node.getRenderPos().equals(ZERO) || node.isOccluded(width, height)) {
							continue;
						}

						if (fl.getFloor() > 0) {
							Composite comp = g2d.getComposite();
							if (!node.canReach(playerNode)) {
								g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
								g2d.setColor(Color.DARK_GRAY);
							} else {
								float hue = switch (node.getType()) {
									case EVENT -> 50;
									case REST -> 100;
									case DANGER -> 20;
									case BOSS -> 360;
									default -> 0;
								} / 360f;

								g2d.setColor(Color.getHSBColor(hue, hue == 0 ? 0 : 0.8f, 0.2f));
							}

							g2d.setStroke(new WobbleStroke(bgRng, Node.NODE_RADIUS / 3 * 2));
							g2d.drawLine(
									node.getRenderPos().x - Node.NODE_RADIUS / 3, node.getRenderPos().y,
									node.getRenderPos().x + Node.NODE_RADIUS / 3, node.getRenderPos().y
							);
							g2d.setComposite(comp);
						} else {
							int ground = sliceHeight + 50;

							g2d.setColor(new Color(41, 90, 24));
							g2d.setStroke(new BasicStroke(6));
							g2d.drawLine(0, ground, width, ground);

							g2d.setColor(Color.DARK_GRAY);
							g2d.setStroke(new BasicStroke(Node.NODE_RADIUS, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
							g2d.drawLine(node.getRenderPos().x, 10, node.getRenderPos().x, ground);
						}
					}
				} else {
					for (Node node : nds) {
						node.calcColor();
						if (node.getRenderPos().equals(ZERO) || node.isOccluded(width, height)) {
							continue;
						}

						for (Node parent : node.getParents()) {
							if (!parent.isRendered() && parent.isOccluded(width, height)) {
								parent.render(g2d, playerNode);
							}
						}

						node.render(g2d, playerNode);
					}
				}
			}
		}

		g2d.dispose();
		return bi;
	}

	public static void generateRandom(AreaMap m) {
		for (int i = -RENDER_FLOORS; i <= RENDER_FLOORS; i++) {
			int depth = m.getRenderFloor() + i;
			if (depth < -1) continue;

			Floor fl = new Floor(depth);
			m.addFloor(fl);

			List<Sublevel> sublevels = fl.getSublevels();
			for (int j = 0; j < sublevels.size(); j++) {
				Sublevel sub = sublevels.get(j);
				if (sublevels.size() == 1) {
					sub.addNode(NodeType.NONE);
					continue;
				}

				Sublevel prev;
				if (sub.getSublevel() == 0) {
					Floor floor = m.getFloor(fl.getFloor() - 1);
					if (floor != null) {
						prev = floor.getSublevel(floor.size() - 1);
					} else {
						prev = null;
					}
				} else {
					prev = sublevels.get(sub.getSublevel() - 1);
				}

				if (prev != null) {
					int nodeCount;
					if (sub.getSublevel() == 0 || sub.getSublevel() == fl.size() - 1) {
						nodeCount = 1;
					} else {
						int min = Math.min(1 + fl.getFloor() / 20, Sublevel.MAX_NODES / 3);
						int max = Math.min(3 + fl.getFloor() / 10, Sublevel.MAX_NODES);
						if (max > 5 && 5d / prev.size() > 1) {
							max = 5;
						}

						nodeCount = fl.getRng().nextInt(min, max + 1);
					}

					float part = ((float) prev.size() / nodeCount);
					for (int k = 0; k < nodeCount; k++) {
						List<Node> parents = new ArrayList<>(prev.getNodes().subList((int) (part * k), (int) Math.ceil(part * (k + 1))));
						List<Node> blkPar = parents.stream()
								.filter(p -> !p.getChildren().isEmpty())
								.toList();

						if (!blkPar.isEmpty()) {
							while (parents.size() > 1 && fl.getRng().nextDouble() > 1d / (parents.size() + 1)) {
								Node parent = blkPar.get(fl.getRng().nextInt(0, blkPar.size()));
								parents.remove(parent);
							}
						}

						if (sub.getSublevel() > 1 && j > 0) {
							Sublevel leap = sublevels.get(sub.getSublevel() - 2);
							if (leap.size() > 0 && !prev.hasLeapNode()) {
								double leapRoll = fl.getRng().nextDouble();
								if (leapRoll < 0.33) {
									Node leapNode = null;
									if (nodeCount > prev.size() && leap.size() > prev.size()) {
										if (k == 0) {
											leapNode = leap.getNode(0);
										} else if (k == nodeCount - 1) {
											leapNode = leap.getNode(leap.size() - 1);
										}
									}

									if (nodeCount % 2 == 1 && k == nodeCount / 2 && leap.size() % 2 == 1) {
										if (prev.size() % 2 != leap.size() % 2) {
											leapNode = leap.getNode(leap.size() / 2);
										}
									}

									if (leapNode != null && leapNode.getChildren().size() < 5) {
										parents.add(leapNode);
									}
								}
							}
						}

						sub.addNode(parents);
					}
				}
			}

			fl.generateEvents(1 / 3d, 1);
		}
	}
}
