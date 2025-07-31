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
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.localized.LocalizedUnique;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "unique", schema = "dunhun")
public class Unique extends DAO<Unique> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedUnique> infos = new HashSet<>();

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "basetype_id")
	@Fetch(FetchMode.JOIN)
	private Basetype basetype;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "affixes", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray affixes;

	@Column(name = "weight", nullable = false)
	private int weight;

	private transient List<Affix> affixCache;

	public String getId() {
		return id;
	}

	public LocalizedUnique getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public Basetype getBasetype() {
		return basetype;
	}

	public List<Affix> getAffixes() {
		if (affixCache != null) return affixCache;

		return affixCache = DAO.queryAll(Affix.class, "SELECT a FROM Affix a WHERE a.id IN ?1", affixes);
	}

	public Gear asGear() {
		return asGear(null);
	}

	public Gear asGear(Hero owner) {
		Gear g = new Gear(owner, this);
		for (Affix a : getAffixes()) {
			g.getAffixes().add(new GearAffix(g, a));
		}

		return g;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Unique that = (Unique) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	public static Unique getRandom(Actor<?> source) {
		Dunhun game = null;
		int dropLevel = Integer.MAX_VALUE;
		if (source != null && source.getGame() != null) {
			int area = source.getGame().getAreaLevel();
			game = source.getGame();
			dropLevel = area + switch (source.getRarityClass()) {
				case NORMAL -> 0;
				case MAGIC -> 1;
				case RARE -> 2;
				case UNIQUE -> 5;
			};
		}

		List<Object[]> uqs = DAO.queryAllUnmapped("""
				SELECT u.id
				     , u.weight
				FROM "unique" u
				INNER JOIN basetype b ON u.basetype_id = b.id
				WHERE u.weight > 0
				  AND b.req_level <= ?1
				""", dropLevel);
		if (uqs.isEmpty()) return null;

		RandomList<String> rl;
		if (game != null) {
			rl = new RandomList<>(game.getNodeRng());
		} else {
			rl = new RandomList<>();
		}

		for (Object[] a : uqs) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		if (rl.entries().isEmpty()) return null;
		return DAO.find(Unique.class, rl.get());
	}
}
