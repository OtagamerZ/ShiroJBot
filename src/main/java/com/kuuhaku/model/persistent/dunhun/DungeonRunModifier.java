package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.records.id.DungeonRunModifierId;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "dungeon_run_modifier", schema = "dunhun")
public class DungeonRunModifier extends DAO<DungeonRunModifier> {
	@EmbeddedId
	private DungeonRunModifierId id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "modifier_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("modifierId")
	private RunModifier modifier;

	public DungeonRunModifier() {
	}

	public DungeonRunModifier(DungeonRun parent, RunModifier modifier) {
		this.id = new DungeonRunModifierId(parent.getId(), modifier.getId());
		this.modifier = modifier;
	}

	public DungeonRunModifierId getId() {
		return id;
	}

	public RunModifier getModifier() {
		return modifier;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		DungeonRunModifier that = (DungeonRunModifier) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
