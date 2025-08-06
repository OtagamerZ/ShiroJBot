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
import com.kuuhaku.model.common.dunhun.context.SkillContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.CpuRule;
import com.kuuhaku.model.persistent.localized.LocalizedSkill;
import com.kuuhaku.model.records.dunhun.Requirements;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.helpers.MessageFormatter;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "skill", schema = "dunhun")
public class Skill extends DAO<Skill> implements Cloneable {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	private SkillStats stats = new SkillStats();

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedSkill> infos = new HashSet<>();

	@Embedded
	private Requirements requirements;

	@Transient
	private transient JSONObject ctxVar = new JSONObject();
	private transient int cd = 0;

	public String getId() {
		return id;
	}

	public SkillStats getStats() {
		return stats;
	}

	public String getName(I18N locale) {
		return getInfo(locale).getName();
	}

	public String getDescription(I18N locale) {
		return getDescription(locale, null);
	}

	public String getDescription(I18N locale, Actor<?> source) {
		if (source == null) {
			return MessageFormatter.basicArrayFormat(
					getInfo(locale).getDescription(),
					stats.getValues().stream()
							.map("**(%s)**"::formatted)
							.toArray()
			);
		}

		return MessageFormatter.basicArrayFormat(
				getInfo(locale).getDescription(),
				stats.getValues().stream()
						.map(v -> "**%s**".formatted(v.valueFor(this, source)))
						.toArray()
		);
	}

	public LocalizedSkill getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public List<Actor<?>> getTargets(Actor<?> source) {
		return stats.getTargets(id, source);
	}

	public CpuRule canCpuUse(Actor<?> source, Actor<?> target) {
		return stats.canCpuUse(id, source, target);
	}

	public void execute(Actor<?> source, Actor<?> target) {
		if (stats.getEffect() == null) return;

		try {
			Utils.exec(id, stats.getEffect(), Map.of(
					"ctx", new SkillContext(source, target, getValues(source), ctxVar)
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute skill {}", id, e);
		}
	}

	public List<Integer> getValues(Actor<?> source) {
		return stats.getValues().stream()
				.map(v -> v.valueFor(this, source))
				.toList();
	}

	public Requirements getRequirements() {
		return requirements;
	}

	public int getCooldown() {
		return cd;
	}

	public void setCooldown(int cd) {
		this.cd = Math.max(this.cd, cd);
	}

	public void reduceCd() {
		cd = Math.max(0, cd - 1);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Skill affix = (Skill) o;
		return Objects.equals(id, affix.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public Skill clone() {
		try {
			Skill clone = (Skill) super.clone();
			clone.ctxVar = new JSONObject(ctxVar);

			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
