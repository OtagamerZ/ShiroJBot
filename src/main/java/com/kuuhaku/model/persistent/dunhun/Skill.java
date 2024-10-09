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
import com.kuuhaku.model.enums.dunhun.AttrType;
import com.kuuhaku.model.enums.dunhun.WeaponType;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.localized.LocalizedSkill;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;
import org.intellij.lang.annotations.Language;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "skill", schema = "dunhun")
public class Skill extends DAO<Skill> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedSkill> infos = new HashSet<>();

	@Column(name = "ap_cost", nullable = false)
	private int apCost;

	@Column(name = "cooldown", nullable = false)
	private int cooldown;

	@Language("Groovy")
	@Column(name = "effect", nullable = false, columnDefinition = "TEXT")
	private String effect;

	@Language("Groovy")
	@Column(name = "targeter", columnDefinition = "TEXT")
	private String targeter;

	@Language("Groovy")
	@Column(name = "cpu_rule", columnDefinition = "TEXT")
	private String cpuRule;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "req_weapons", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray reqWeapons;

	@Embedded
	private Attributes requirements;

	private transient int cd = 0;

	public String getId() {
		return id;
	}

	public String getName(I18N locale) {
		return getInfo(locale).getName();
	}

	public String getDescription(I18N locale) {
		return getInfo(locale).getDescription().replaceAll("\\{(\\d+)}", "$1");
	}

	public LocalizedSkill getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.findAny().orElseThrow();
	}

	public int getApCost() {
		return apCost;
	}

	public int getCooldown() {
		return cooldown;
	}

	public void execute(I18N locale, Combat combat, Actor source, Actor target) {
		List<Integer> values = new ArrayList<>();

		if (source instanceof Hero h) {
			Attributes attr = h.getAttributes();
			String desc = getInfo(locale).getDescription();

			double scale;
			String type = Utils.extract(desc, "(?<=^\\()\\w+(?=\\))");
			if (type != null) {
				scale = 1 + switch (AttrType.valueOf(type.toUpperCase())) {
					case STR -> attr.str();
					case DEX -> attr.dex();
					case WIS -> attr.wis();
					case VIT -> attr.vit();
				} / 10d;
			} else {
				scale = 1;
			}

			Utils.regex(desc, "\\{(\\d+)}").results().forEach(v -> {
				int val = (int) (Integer.parseInt(v.group(1)) * scale);
				values.add(val);
			});
		}

		try {
			Utils.exec(id, effect, Map.of(
					"locale", locale,
					"combat", combat,
					"actor", source,
					"target", target,
					"values", values
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute skill {}", id, e);
		}
	}

	public List<Actor> getTargets(Combat combat, Actor source) {
		if (targeter == null) return List.of(source);

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

	public Boolean canCpuUse(Combat combat, Monster source) {
		if (cpuRule == null) return null;

		try {
			Object out = Utils.exec(id, cpuRule, Map.of(
					"combat", combat,
					"actor", source
			));

			if (out instanceof Boolean b) return b;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to check CPU rule {}", id, e);
		}

		return null;
	}

	public Attributes getRequirements() {
		return requirements;
	}

	public Set<WeaponType> getReqWeapons() {
		Set<WeaponType> out = EnumSet.noneOf(WeaponType.class);
		for (int i = 0; i < reqWeapons.size(); i++) {
			WeaponType type = reqWeapons.getEnum(WeaponType.class, i);
			if (type != null) out.add(type);
		}

		return out;
	}

	public int getCd() {
		return cd;
	}

	public void setCd(int cd) {
		this.cd = Math.max(0, cd);
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
}
