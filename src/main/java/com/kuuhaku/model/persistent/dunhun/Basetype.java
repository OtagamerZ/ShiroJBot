/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedBasetype;
import com.kuuhaku.model.records.dunhun.GearStats;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "basetype", schema = "dunhun")
public class Basetype extends DAO<Basetype> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedBasetype> infos = new HashSet<>();

	@Embedded
	private GearStats stats;

	public String getId() {
		return id;
	}

	public LocalizedBasetype getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public String getIcon() {
		return stats.gearType().getIcon();
	}

	public GearStats getStats() {
		return stats;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Basetype that = (Basetype) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	public static Basetype getRandom(Actor<?> source) {
		int dropLevel = Integer.MAX_VALUE;
		if (source != null && source.getGame() != null) {
			int area = source.getGame().getAreaLevel();
			dropLevel = area + switch (source.getRarityClass()) {
				case NORMAL -> 0;
				case MAGIC -> 1;
				case RARE -> 2;
				case UNIQUE -> 5;
			};
		}

		List<Object[]> bases = DAO.queryAllUnmapped("""
				SELECT id
				     , weight
				FROM basetype
				WHERE weight > 0
				  AND req_level <= ?1
				""", dropLevel
		);
		if (bases.isEmpty()) return null;

		RandomList<String> rl = new RandomList<>();
		for (Object[] a : bases) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		if (rl.entries().isEmpty()) return null;
		return DAO.find(Basetype.class, rl.get());
	}
}
