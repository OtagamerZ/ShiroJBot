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
import com.kuuhaku.model.common.dunhun.HeroModifiers;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "monster", schema = "dunhun")
public class Monster extends DAO<Monster> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	private MonsterStats stats;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedMonster> infos = new HashSet<>();

	@Transient
	private final HeroModifiers modifiers = new HeroModifiers();

	public String getId() {
		return id;
	}

	public MonsterStats getStats() {
		return stats;
	}

	public LocalizedMonster getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.findAny().orElseThrow();
	}

	public HeroModifiers getModifiers() {
		return modifiers;
	}

	public Senshi asSenshi(I18N locale) {
		Senshi s = new Senshi(id, stats.getRace());
		CardAttributes base = s.getBase();

		base.setAtk(stats.getAttack());
		base.setDfs(stats.getDefense());
		base.setDodge(stats.getDodge());
		base.setParry(stats.getParry());

		base.getTags().add("MONSTER");
		s.getStats().setDescription(getInfo(locale).getDescription());

		return s;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Monster monster = (Monster) o;
		return Objects.equals(id, monster.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
