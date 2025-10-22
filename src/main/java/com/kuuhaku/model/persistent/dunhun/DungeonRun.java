package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.dunhun.AreaMap;
import com.kuuhaku.model.common.dunhun.Node;
import com.kuuhaku.model.records.id.DungeonRunId;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


@Entity
@Table(name = "dungeon_run", schema = "dunhun")
public class DungeonRun extends DAO<DungeonRun> {
	@EmbeddedId
	private DungeonRunId id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "hero_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("heroId")
	private Hero hero;

	@ManyToOne(optional = false)
	@JoinColumn(name = "dungeon_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("dungeonId")
	private Dungeon dungeon;

	@Column(name = "seed", nullable = false)
	private int seed = ThreadLocalRandom.current().nextInt();

	@Column(name = "floor", nullable = false)
	private int floor = 0;

	@Column(name = "sublevel", nullable = false)
	private int sublevel = 0;

	@Column(name = "path", nullable = false)
	private int path = 0;

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private Set<DungeonRunPlayer> players = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "hero_id", referencedColumnName = "hero_id"),
			@JoinColumn(name = "dungeon_id", referencedColumnName = "dungeon_id")
	})
	private Set<DungeonRunModifier> modifiers = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "node", nullable = false)
	@CollectionTable(
			schema = "dunhun",
			name = "dungeon_run_visited_node",
			joinColumns = {
					@JoinColumn(name = "hero_id"),
					@JoinColumn(name = "dungeon_id")
			}
	)
	private Set<String> visitedNodes = new HashSet<>();

	public DungeonRun() {
	}

	public DungeonRun(Hero hero, Dungeon dungeon) {
		this.id = new DungeonRunId(hero.getId(), dungeon.getId());
		this.hero = hero;
		this.dungeon = dungeon;
	}

	public DungeonRunId getId() {
		return id;
	}

	public Hero getHero() {
		return hero;
	}

	public Dungeon getDungeon() {
		return dungeon;
	}

	public Set<String> getVisitedNodes() {
		Hibernate.initialize(visitedNodes);
		return visitedNodes;
	}

	public int getSeed() {
		return seed;
	}

	public int getFloor() {
		return floor;
	}

	public void setFloor(int floor) {
		this.floor = floor;
	}

	public int getSublevel() {
		return sublevel;
	}

	public void setSublevel(int sublevel) {
		this.sublevel = sublevel;
	}

	public int getPath() {
		return path;
	}

	public void setPath(int path) {
		this.path = path;
	}

	public int getPathHash() {
		return Objects.hash(floor, sublevel, path);
	}

	public void setNode(Node node) {
		floor = node.getSublevel().getFloor().getFloor();
		sublevel = node.getSublevel().getSublevel();
		path = node.getPath();

		AreaMap map = node.getSublevel().getFloor().getMap();
		map.getRenderFloor().set(floor);

		int subOffset = sublevel / map.getAreasPerFloor();
		map.getRenderSublevel().set(subOffset * map.getAreasPerFloor());

		visitedNodes.add(node.getId());
	}

	public Set<DungeonRunPlayer> getPlayers() {
		return players;
	}

	public Set<DungeonRunModifier> getModifiers() {
		return modifiers;
	}

	public boolean addModifier(RunModifier modifier) {
		String family = modifier.getModFamily();

		Iterator<DungeonRunModifier> it = modifiers.iterator();
		while (it.hasNext()) {
			RunModifier mod = it.next().getModifier();
			if (mod.getId().equals(modifier.getId())) return false;

			if (mod.getModFamily().equals(family)) {
				if (mod.getWeight() > modifier.getWeight()) {
					it.remove();
					break;
				} else {
					return false;
				}
			}
		}

		return modifiers.add(new DungeonRunModifier(this, modifier));
	}

	public AreaMap getMap() {
		return new AreaMap(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		DungeonRun that = (DungeonRun) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
