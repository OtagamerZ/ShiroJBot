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
import com.kuuhaku.model.common.dunhun.AreaMap;
import com.kuuhaku.model.common.dunhun.context.DungeonContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.localized.LocalizedDungeon;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;
import org.intellij.lang.annotations.Language;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "dungeon", schema = "dunhun")
public class Dungeon extends DAO<Dungeon> {
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

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "monster_pool", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray monsterPool = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "required_dungeons", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray requiredDungeons = new JSONArray();

	@Column(name = "area_level")
	private int areaLevel = 1;

	@Column(name = "areas_per_floor", nullable = false)
	private int areasPerFloor = AreaMap.LEVELS_PER_FLOOR;

	@Column(name = "hardcore")
	private boolean hardcore;

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
				.findAny()
				.orElseGet(() -> new LocalizedDungeon(locale, id, id + ":" + locale, id + ":" + locale));
	}

	public JSONArray getMonsterPool() {
		return monsterPool;
	}

	public JSONArray getRequiredDungeons() {
		return requiredDungeons;
	}

	public int getAreaLevel() {
		return areaLevel;
	}

	public int getAreasPerFloor() {
		return areasPerFloor;
	}

	public boolean isHardcore() {
		return hardcore;
	}

	public boolean isInfinite() {
		return script == null || script.isBlank();
	}

	public AreaMap init(Dunhun game, DungeonRun run) {
		if (script == null || script.isBlank()) return run.getMap();

		try {
			return new AreaMap(
					run, areasPerFloor,
					(_, m) -> Utils.exec(id, script, Map.of(
							"ctx", new DungeonContext(game, this, m)
					))
			);
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to process dungeon {}", id, e);
			return null;
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
}
