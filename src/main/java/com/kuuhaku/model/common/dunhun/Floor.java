package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.model.persistent.dunhun.RunModifier;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Floor {
	private final AreaMap map;
	private final int floor;
	private final int seed;
	private final Random rng;
	private final Sublevel[] sublevels;
	private final Set<Node> eventNodes = new HashSet<>();
	private final Set<RunModifier> modifiers = new LinkedHashSet<>();
	private int visionLimit;
	private boolean hiddenNodes;

	public Floor(AreaMap map, int floor) {
		this.map = map;
		this.floor = floor;
		this.seed = Utils.generateSeed(DigestUtils.getMd5Digest(), map.getSeed(), floor);
		this.rng = new Random(this.seed);
		this.sublevels = new Sublevel[floor <= 0 ? 1 : map.getAreasPerFloor()];

		for (int i = 0; i < this.sublevels.length; i++) {
			this.sublevels[i] = new Sublevel(this, i);
		}
	}

	public AreaMap getMap() {
		return map;
	}

	public Sublevel getSublevel(int sublevel) {
		return sublevels[sublevel];
	}

	public List<Sublevel> getSublevels() {
		return List.of(sublevels);
	}

	public Set<Node> getEventNodes() {
		return eventNodes;
	}

	public Set<RunModifier> getModifiers() {
		return modifiers;
	}

	public void generateEvents(double eventRatio, int restSpots) {
		if (sublevels.length < 3) return;
		eventNodes.clear();

		List<Node> nodes = Stream.of(sublevels)
				.skip(1).limit(sublevels.length - 2)
				.flatMap(sl -> sl.getNodes().stream())
				.collect(Collectors.toCollection(ArrayList::new));

		int events = (int) (nodes.size() * eventRatio);
		for (int i = 0; i < events; i++) {
			Node chosen = nodes.get(rng.nextInt(nodes.size()));
			if (chosen.getParents().stream().anyMatch(p -> p.getType() != NodeType.NONE && p.getChildren().size() == 1)) {
				continue;
			}

			if (chosen.getParents().stream().anyMatch(p -> chosen.getSublevel().getSublevel() - p.getSublevel().getSublevel() > 1)) {
				chosen.setType(NodeType.DANGER);
			} else {
				chosen.setType(NodeType.EVENT);
				eventNodes.add(chosen);
			}

			nodes.remove(chosen);
		}

		List<Node> eNodes = eventNodes.stream()
				.filter(n -> n.getSublevel().getSublevel() >= n.getSublevel().getFloor().size() / 2)
				.collect(Collectors.toCollection(ArrayList::new));

		for (int i = 0; i < restSpots; i++) {
			if (eNodes.isEmpty()) break;

			Node chosen = eNodes.remove(rng.nextInt(eNodes.size()));
			chosen.setType(NodeType.REST);
		}
	}

	public void generateModifiers(Dunhun game) {
		modifiers.clear();

		int mods = 0;
		if (game.getAreaLevel() >= Dunhun.LEVEL_BRUTAL) {
			mods = 4;
		} else if (game.getAreaLevel() >= Dunhun.LEVEL_HARD) {
			mods = 2;
		}

		for (int i = 0; i < mods && Calc.chance(100d / (modifiers.size() + 1), rng); i++) {
			RunModifier mod = RunModifier.getRandom(game, this);
			if (mod == null) break;

			modifiers.add(mod);
		}
	}

	public int getVisionLimit() {
		return visionLimit;
	}

	public void setVisionLimit(int visionLimit) {
		this.visionLimit = visionLimit;
	}

	public boolean areNodesHidden() {
		return hiddenNodes;
	}

	public void setHiddenNodes(boolean hiddenNodes) {
		this.hiddenNodes = hiddenNodes;
	}

	public List<Node> getNodes() {
		return Stream.of(sublevels)
				.flatMap(sl -> sl.getNodes().stream())
				.toList();
	}

	public int getFloor() {
		return floor;
	}

	public int getSeed() {
		return seed;
	}

	public Random getRng() {
		return rng;
	}

	public int size() {
		return sublevels.length;
	}

	public int depth() {
		if (floor < 0) return 0;
		return (floor - 1) * map.getAreasPerFloor();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Floor floor1 = (Floor) o;
		return floor == floor1.floor;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(floor);
	}
}
