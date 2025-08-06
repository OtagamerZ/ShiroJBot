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
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.context.SkillContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.CpuRule;
import com.kuuhaku.model.persistent.localized.LocalizedConsumable;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "consumable", schema = "dunhun")
public class Consumable extends DAO<Consumable> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	private SkillStats stats = new SkillStats();

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedConsumable> infos = new HashSet<>();

	@Column(name = "price")
	private Integer price;

	@Column(name = "weight", nullable = false)
	private int weight;

	private transient int count;

	public String getId() {
		return id;
	}

	public SkillStats getStats() {
		return stats;
	}

	public String getName(I18N locale) {
		return getInfo(locale).setUwu(locale.isUwu()).getName();
	}

	public String getDescription(I18N locale) {
		return getInfo(locale).setUwu(locale.isUwu()).getDescription();
	}

	public LocalizedConsumable getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public int getPrice() {
		return price;
	}

	public int getCount() {
		return count;
	}

	public void add(int amount) {
		count += amount;
	}

	public void consume(int amount) {
		count = Math.max(0, count - amount);
	}

	public List<Actor<?>> getTargets(Actor<?> source) {
		return stats.getTargets(id, source);
	}

	public CpuRule canCpuUse(Actor<?> source, Actor<?> target) {
		return stats.canCpuUse(id, source, target);
	}

	public void execute(Actor<?> source, Actor<?> target) {
		if (count <= 0 || stats.getEffect() == null) return;

		try {
			Utils.exec(id, stats.getEffect(), Map.of(
					"ctx", new SkillContext(source, target)
			));
			count--;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute consumable {}", id, e);
		}
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

	public static Consumable getRandom() {
		List<Object[]> affs = DAO.queryAllUnmapped("""
				SELECT id
				     , weight
				FROM consumable
				WHERE weight > 0
				""");
		if (affs.isEmpty()) return null;

		RandomList<String> rl = new RandomList<>();
		for (Object[] a : affs) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		if (rl.entries().isEmpty()) return null;
		return DAO.find(Consumable.class, rl.get());
	}
}
