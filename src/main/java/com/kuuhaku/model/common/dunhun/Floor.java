package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.dunhun.NodeType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Floor {
	public static final int AREAS_PER_FLOOR = 10;

	private final int floor;
	private final Random rng;
	private final Sublevel[] sublevels;
	private final Set<Node> eventNodes = new HashSet<>();

	public Floor(int floor) {
		this(floor, floor <= 0 ? 1 : AREAS_PER_FLOOR);
	}

	public Floor(int floor, int levels) {
		this.floor = floor;
		this.rng = new Random(this.floor);
		this.sublevels = new Sublevel[levels];

		for (int i = 0; i < sublevels.length; i++) {
			sublevels[i] = new Sublevel(this, i);
		}
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

	public List<Node> getNodes() {
		return Stream.of(sublevels)
				.flatMap(sl -> sl.getNodes().stream())
				.toList();
	}

	public int getFloor() {
		return floor;
	}

	public Random getRng() {
		return rng;
	}

	public int size() {
		return sublevels.length;
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
