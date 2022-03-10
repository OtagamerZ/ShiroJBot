package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "debuff")
public class Debuff {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String name;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String effect;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 60")
	private long duration;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEffect() {
		return effect;
	}

	public void setEffect(String effect) {
		this.effect = effect;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Debuff debuff = (Debuff) o;
		return id == debuff.id && duration == debuff.duration && Objects.equals(name, debuff.name) && Objects.equals(description, debuff.description) && Objects.equals(effect, debuff.effect);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, effect, duration);
	}
}
