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
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedConsumable;
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
@Table(name = "consumable", schema = "dunhun")
public class Consumable extends DAO<Consumable> implements Comparable<Consumable> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedConsumable> infos = new HashSet<>();

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	@Language("Groovy")
	@Column(name = "targeter", columnDefinition = "TEXT")
	private String targeter;

	public String getId() {
		return id;
	}

	public LocalizedConsumable getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.findAny().orElseThrow();
	}

	public void execute(I18N locale, Combat combat, Actor source, Actor target) {
		try {
			Utils.exec(id, effect, Map.of(
					"locale", locale,
					"combat", combat,
					"actor", source,
					"target", target,
					"pow", source instanceof Hero h ? (1 + h.getAttributes().wis() / 10d) : 1
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute skill {}", id, e);
		}
	}

	public List<Actor> getTargets(Combat combat, Actor source) {
		List<Actor> out = new ArrayList<>();

		try {
			Utils.exec(id, targeter, Map.of(
					"combat", combat,
					"actor", source,
					"targets", out
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to load targets {}", id, e);
		}

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Consumable affix = (Consumable) o;
		return Objects.equals(id, affix.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public int compareTo(@NotNull Consumable o) {
		return id.compareTo(o.id);
	}
}
