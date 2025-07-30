package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.dunhun.AreaMap;
import com.kuuhaku.model.records.id.DungeonRunId;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "dungeon_run_modifier",
			schema = "dunhun",
			joinColumns = {
					@JoinColumn(name = "hero_id", referencedColumnName = "hero_id"),
					@JoinColumn(name = "dungeon_id", referencedColumnName = "dungeon_id")
			},
			inverseJoinColumns = @JoinColumn(name = "modifier_id")
	)
	@Fetch(FetchMode.SUBSELECT)
	private List<RunModifier> modifiers = new ArrayList<>();

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

	public List<RunModifier> getModifiers() {
		return modifiers;
	}

	public AreaMap getMap() {
		return new AreaMap(seed, floor);
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
