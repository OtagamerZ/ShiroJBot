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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedDungeon;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "dungeon", schema = "dunhun")
public class Dungeon extends DAO<Dungeon> implements Iterable<Runnable> {
	public static final Dungeon DUEL = new Dungeon("DUEL", 1);

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedDungeon> infos = new HashSet<>();

	@Language("Groovy")
	@Column(name = "script", columnDefinition = "TEXT")
	private String script;

	@Column(name = "area_level")
	private int areaLevel;

	private transient final List<Runnable> floors = new ArrayList<>();

	public Dungeon() {
	}

	public Dungeon(String id, int areaLevel) {
		this.id = id;
		this.areaLevel = areaLevel;
	}

	public String getId() {
		return id;
	}

	public LocalizedDungeon getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public int getAreaLevel() {
		return areaLevel;
	}

	public void init(I18N locale, Dunhun dungeon) {
		try {
			floors.clear();
			Utils.exec(id, script, Map.of(
					"locale", locale,
					"dungeon", dungeon,
					"floors", floors
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to process dungeon {}", id, e);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Dungeon affix = (Dungeon) o;
		return Objects.equals(id, affix.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public @NotNull Iterator<Runnable> iterator() {
		return floors.iterator();
	}
}
