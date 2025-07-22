package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.records.dunhun.RaceValues;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.Objects;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "race_bonus", schema = "dunhun")
public class RaceBonus extends DAO<RaceBonus> {
	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race id;

	@Embedded
	private RaceValues values = new RaceValues();

	public Race getId() {
		return id;
	}

	public RaceValues getValues() {
		return values;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		RaceBonus raceBonus = (RaceBonus) o;
		return id == raceBonus.id;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
