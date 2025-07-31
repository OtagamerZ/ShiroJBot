package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.records.id.DungeonRunPlayerId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "dungeon_run_player", schema = "dunhun")
public class DungeonRunPlayer extends DAO<DungeonRunPlayer> {
	@EmbeddedId
	private DungeonRunPlayerId id;

	@Column(name = "hp", nullable = false)
	private int hp;

	public DungeonRunPlayer() {
	}

	public DungeonRunPlayer(DungeonRun parent, Hero player) {
		this.id = new DungeonRunPlayerId(parent.getId(), player.getId());
		this.hp = player.getHp();
	}

	public DungeonRunPlayerId getId() {
		return id;
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int hp) {
		this.hp = hp;
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
