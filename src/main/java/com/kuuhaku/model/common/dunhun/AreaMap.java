package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.model.persistent.dunhun.DungeonRun;
import com.kuuhaku.model.persistent.dunhun.DungeonRunOutcome;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.RunModifier;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.WobbleStroke;
import kotlin.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class AreaMap {
	public static final int RENDER_FLOORS = 1;
	public static final int LEVELS_PER_FLOOR = 10;
	public static final int RENDER_DEPTH = LEVELS_PER_FLOOR;
	public static final int AVATAR_RADIUS = 50;
	private static final Point ZERO = new Point();

	private final int areasPerFloor;
	private final BiConsumer<Dunhun, AreaMap> generator;
	private final DungeonRun run;
	private final TreeMap<Integer, Floor> floors = new TreeMap<>();
	private final AtomicInteger renderFloor = new AtomicInteger(0);
	private final AtomicInteger renderSublevel = new AtomicInteger(0);

	private Pair<Integer, Node> pnCache;

	public AreaMap(DungeonRun run, int areasPerFloor, BiConsumer<Dunhun, AreaMap> generator) {
		this.generator = generator;
		this.areasPerFloor = areasPerFloor;
		this.run = run;
	}

	public AreaMap(DungeonRun run) {
		this(run, LEVELS_PER_FLOOR, AreaMap::generateRandom);
		this.renderFloor.set(run.getFloor());
	}

	public int getSeed() {
		return run.getSeed();
	}

	public int getAreasPerFloor() {
		return areasPerFloor;
	}

	public List<Floor> getFloors() {
		return List.copyOf(floors.values());
	}

	public Node newRoot() {
		Floor floor = new Floor(this, 0);
		Node root = floor.getSublevel(0).newNode(NodeType.NONE);
		addFloor(floor);
		return root;
	}

	public Floor newFloor() {
		Floor floor = new Floor(this, floors.size());
		addFloor(floor);
		return floor;
	}

	public void addFloor(Floor floor) {
		floors.put(floor.getNumber(), floor);
	}

	public Floor getFloor() {
		return getFloor(renderFloor.get());
	}

	public Floor getFloor(int depth) {
		return floors.get(depth);
	}

	public Node getPlayerNode() {
		if (!floors.containsKey(run.getFloor())) return null;

		if (pnCache != null) {
			if (pnCache.getFirst() == run.getPathHash()) {
				return pnCache.getSecond();
			}
		}

		Floor fl = getFloor(run.getFloor());
		if (run.getSublevel() >= fl.size()) {
			run.setSublevel(fl.size() - 1);
		}

		Sublevel sub = fl.getSublevel(run.getSublevel());
		if (run.getPath() >= sub.size()) {
			run.setPath(sub.size() - 1);
		}

		pnCache = new Pair<>(run.getPathHash(), sub.getNode(run.getPath()));
		return pnCache.getSecond();
	}

	public AtomicInteger getRenderFloor() {
		return renderFloor;
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

	public DungeonRun getRun() {
		return run;
	}

	public List<Hero> getHeroesAt(int floor, int sublevel) {
		return DAO.queryAll(Hero.class, """
				SELECT hero
				FROM DungeonRun r
				WHERE id.dungeonId = ?1
				  AND floor = ?2
				  AND sublevel = ?3
				  AND players.size = 1
				""", run.getId().dungeonId(), floor, sublevel);
	}

	public Map<Integer, List<Hero>> getHeroesAt(int floor) {
		List<Object[]> runs = DAO.queryAllUnmapped("""
				SELECT r.sublevel
				     , r.hero_id
				FROM dungeon_run r
				INNER JOIN dungeon_run_player rp ON rp.dungeon_id = r.dungeon_id AND rp.hero_id = r.hero_id
				WHERE r.dungeon_id = ?1
				  AND r.floor = ?2
				GROUP BY r.hero_id, r.sublevel
				HAVING count(rp.player_id) = 1
				""", run.getId().dungeonId(), floor);

		Map<Integer, List<Hero>> heroes = new HashMap<>();
		for (Object[] o : runs) {
			heroes.computeIfAbsent((Integer) o[0], _ -> new ArrayList<>())
					.add(DAO.find(Hero.class, o[1]));
		}

		return heroes;
	}

	public void generate(Dunhun game) {
		floors.clear();
		generator.accept(game, this);
	}

	public BufferedImage render(I18N locale, int width, int height) {
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int floorCount = floors.size();
		if (floorCount == 0) return bi;

		Map<Integer, List<Hero>> runs = getHeroesAt(run.getFloor());

		int sliceHeight = height / RENDER_DEPTH;
		int missingHeight = Math.max(0, (sliceHeight * areasPerFloor) - height);

		Random bgRng = new Random(floors.hashCode());
		Node playerNode = getPlayerNode();
		int visionLimit = playerNode.getSublevel().getFloor().getVisionLimit();

		{
			int y = -sliceHeight * renderSublevel.get();
			if (renderFloor.get() == 0) {
				y += 25;
			}

			if (missingHeight > 0) {
				int floorSize = playerNode.getSublevel().getFloor().size();
				if (floorSize == areasPerFloor) {
					y -= playerNode.getSublevel().getNumber() * missingHeight / (floorSize - 1);
				}
			}

			for (Floor fl : floors.values()) {
				if (fl.getNumber() < renderFloor.get()) continue;

				boolean label = fl.getNumber() > 0;
				if (label) {
					g2d.setColor(Color.DARK_GRAY);
					g2d.drawLine(0, y, width, y);

					g2d.setFont(new Font("Arial", Font.BOLD, 25));

					String text = locale.get("str/dungeon_floor", fl.getNumber());
					g2d.drawString(text,
							width - g2d.getFontMetrics().stringWidth(text) - 5,
							y + g2d.getFontMetrics().getHeight()
					);
					y += 50;
				} else {
					g2d.setColor(new Color(138, 168, 179));
					g2d.fillRect(0, y - 25, width, sliceHeight + 50);
					y += 25;
				}

				if (fl.getNumber() == renderFloor.get()) {
					Floor prev = floors.get(fl.getNumber() - 1);
					if (prev != null && prev.getNumber() >= 0) {
						Sublevel last = prev.getSublevels().get(prev.size() - 1);
						last.placeNodes(width / 2, y - sliceHeight - 50);
					}
				}

				for (Sublevel sub : fl.getSublevels()) {
					sub.placeNodes(width / 2, y + ((fl.getNumber() == 0 ? 25 : 0)));

					List<Hero> runsHere = runs.get(sub.getNumber());
					if (runsHere != null) {
						for (int i = 0; i < Math.min(runsHere.size(), 5); i++) {
							Hero run = runsHere.get(i);
							Graph.applyTransformed(g2d, 5 + (AVATAR_RADIUS + 5) * i, y - AVATAR_RADIUS / 2, g -> {
								BufferedImage avatar = run.getImage();
								if (avatar != null) {
									g.drawImage(avatar, 0, 0,
											AVATAR_RADIUS * avatar.getWidth() / 350, AVATAR_RADIUS,
											null
									);
								}
							});
						}
					}

					y += sliceHeight;
				}
			}
		}

		List<Floor> floors = List.copyOf(this.floors.values());
		Map<Floor, List<Node>> nodes = new TreeMap<>(Comparator.comparingInt(Floor::getNumber));
		for (int i = 0; i < 3; i++) {
			for (Floor fl : floors) {
				List<Node> nds = nodes.computeIfAbsent(fl, _ -> fl.getNodes());
				for (Node node : nds) {
					if (i == 1) node.calcColor();

					int distance = node.travelDistance(playerNode);
					boolean outsideView = visionLimit > 0
							&& !run.getVisitedNodes().contains(node.getId())
							&& (distance > visionLimit || distance == -1);
					boolean occluded = node.isOccluded(width, height) || outsideView;
					if (node.getRenderPos().equals(ZERO) || occluded) {
						node.setWillBeRendered(false);
						continue;
					}

					node.setWillBeRendered(true);
					switch (i) {
						case 0 -> {
							if (fl.getNumber() > 0) {
								Composite comp = g2d.getComposite();
								if (distance == -1) {
									g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
									g2d.setColor(Color.DARK_GRAY);
								} else {
									float hue = switch (node.getType()) {
										case EVENT -> 50;
										case REST -> 100;
										case RETURN -> 250;
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
						case 1 -> {
							for (Node parent : node.getParents()) {
								if (!parent.isPathRendered() && parent.isOccluded(width, height)) {
									parent.renderPath(g2d, distance > 0);
								}
							}

							if (!node.isPathRendered()) {
								node.renderPath(g2d, distance >= 0);
							}
						}
						case 2 -> {
							if (!node.isNodeRendered()) {
								node.renderNode(g2d, playerNode, distance >= 0);
							}
						}
					}
				}
			}
		}

		g2d.dispose();
		return bi;
	}

	public static void generateRandom(Dunhun game, AreaMap m) {
		for (int i = -RENDER_FLOORS; i <= RENDER_FLOORS; i++) {
			int depth = m.renderFloor.get() + i;
			if (depth < -1) continue;

			Floor fl = new Floor(m, depth);
			m.addFloor(fl);

			List<Sublevel> sublevels = fl.getSublevels();
			for (int j = 0; j < sublevels.size(); j++) {
				Sublevel sub = sublevels.get(j);
				if (sublevels.size() == 1) {
					sub.addNode(NodeType.NONE);
					continue;
				}

				Sublevel prev;
				if (sub.getNumber() == 0) {
					Floor floor = m.getFloor(fl.getNumber() - 1);
					if (floor != null) {
						prev = floor.getSublevel(floor.size() - 1);
					} else {
						prev = null;
					}
				} else {
					prev = sublevels.get(sub.getNumber() - 1);
				}

				if (prev != null) {
					int nodeCount;
					if (sub.getNumber() == 0 || sub.getNumber() == fl.size() - 1) {
						nodeCount = 1;
					} else {
						int min = Math.min(1 + fl.getNumber() / 20, Sublevel.MAX_NODES / 3);
						int max = Math.min(3 + fl.getNumber() / 10, Sublevel.MAX_NODES);
						if (max > 5 && 5d / prev.size() > 1) {
							max = 5;
						}

						nodeCount = fl.getRng().nextInt(min, max + 1);
					}

					float part = ((float) prev.size() / nodeCount);
					for (int k = 0; k < nodeCount; k++) {
						Node boss = prev.getNodes().stream()
								.filter(n -> n.getType() == NodeType.BOSS)
								.findFirst().orElse(null);

						if (boss == null) {
							List<Node> parents = new ArrayList<>(prev.getNodes().subList(
									(int) (part * k),
									(int) Math.ceil(part * (k + 1))
							));

							List<Node> blkPar = parents.stream()
									.filter(p -> !p.getChildren().isEmpty())
									.toList();

							if (!blkPar.isEmpty()) {
								while (parents.size() > 1 && fl.getRng().nextDouble() > 1d / (parents.size() + 1)) {
									Node parent = blkPar.get(fl.getRng().nextInt(0, blkPar.size()));
									parents.remove(parent);
								}
							}

							if (sub.getNumber() > 1 && j > 0) {
								Sublevel leap = sublevels.get(sub.getNumber() - 2);
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
						} else {
							sub.addNode(boss);
						}
					}
				}
			}

			if (sublevels.size() > 1) {
				Sublevel last = sublevels.getLast();
				if (last.size() == 1) {
					Node n = last.getNode(0);
					if (n.getType() == NodeType.BOSS) {
						Node ret = last.newNode(NodeType.RETURN, List.of(n));
						ret.setOffsetNode(true);
					}
				}
			}

			int rests;
			int areaLevel = game.getAreaLevel(fl);
			if (areaLevel > Dunhun.LEVEL_BRUTAL) {
				rests = Calc.chance(50, fl.getRng()) ? 1 : 0;
			} else if (areaLevel > Dunhun.LEVEL_HARD) {
				rests = Calc.rng(1, 2, fl.getRng());
			} else {
				rests = 3;
			}

			fl.generateEvents(m.getRun(), 1 / 3d, rests);
		}

		m.getFloor().generateModifiers(game);
		for (RunModifier mod : game.getModifiers()) {
			mod.toEffect(game);
		}

		for (DungeonRunOutcome out : m.getRun().getEventOutcomes()) {
			if (out.isGlobal()) {
				out.apply(game, null);
			} else {
				Floor fl = m.getFloor(out.getFloor());
				if (fl != null) {
					fl.getNodes().stream()
							.filter(n -> n.getId().equals(out.getId().nodeId()))
							.findFirst()
							.ifPresent(node -> out.apply(game, node));
				}
			}
		}
	}
}
