package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.records.id.DungeonRunId;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "dungeon_completion", schema = "dunhun")
public class DungeonCompletion extends DAO<DungeonCompletion> {
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

	public DungeonCompletion() {
	}

	public DungeonCompletion(Hero hero, Dungeon dungeon) {
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

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		DungeonCompletion that = (DungeonCompletion) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
