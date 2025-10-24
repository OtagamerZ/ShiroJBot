package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.records.id.DungeonRunPlayerId;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "dungeon_run_player", schema = "dunhun")
public class DungeonRunPlayer extends DAO<DungeonRunPlayer> {
	@EmbeddedId
	private DungeonRunPlayerId id;

	@ManyToOne(optional = false)
	@JoinColumns({
			@JoinColumn(name = "hero_id", referencedColumnName = "hero_id", updatable = false),
			@JoinColumn(name = "dungeon_id", referencedColumnName = "dungeon_id", updatable = false)
	})
	@Fetch(FetchMode.JOIN)
	@MapsId("runId")
	private DungeonRun parent;

	@ManyToOne(optional = false)
	@JoinColumn(name = "player_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("playerId")
	private Hero player;

	@Column(name = "hp", nullable = false)
	private int hp;

	public DungeonRunPlayer() {
	}

	public DungeonRunPlayer(DungeonRun parent, Hero player) {
		this.id = new DungeonRunPlayerId(parent.getId(), player.getId());
		this.parent = parent;
		this.player = player;
		this.hp = player.getHp();
	}

	public DungeonRunPlayerId getId() {
		return id;
	}

	public DungeonRun getParent() {
		return parent;
	}

	public Hero getPlayer() {
		return player;
	}

	public int getHp() {
		return hp;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		DungeonRunPlayer that = (DungeonRunPlayer) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
