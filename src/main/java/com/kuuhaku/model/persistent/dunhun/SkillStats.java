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
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.context.SkillContext;
import com.kuuhaku.model.enums.dunhun.CpuRule;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.records.dunhun.SkillValue;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.intellij.lang.annotations.Language;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Embeddable
public class SkillStats implements Serializable {
	@Column(name = "cost", nullable = false)
	private int cost;

	@Column(name = "cooldown", nullable = false)
	private int cooldown;

	@Column(name = "efficiency", nullable = false)
	private double efficiency;

	@Column(name = "spell", nullable = false)
	private boolean spell;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "values", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray values = new JSONArray();

	@Language("Groovy")
	@Column(name = "targeter", columnDefinition = "TEXT")
	private String targeter;

	@Language("Groovy")
	@Column(name = "cpu_rule", columnDefinition = "TEXT")
	private String cpuRule;

	@Language("Groovy")
	@Column(name = "effect", nullable = false, columnDefinition = "TEXT")
	private String effect;

	public SkillStats() {
	}

	public int getCost() {
		return cost;
	}

	public int getCooldown() {
		return cooldown;
	}

	public double getEfficiency() {
		return efficiency;
	}

	public boolean isSpell() {
		return spell;
	}

	public List<SkillValue> getValues() {
		List<SkillValue> out = new ArrayList<>();
		for (Object e : values) {
			out.add(SkillValue.parse(String.valueOf(e)));
		}

		return out;
	}

	public List<Actor<?>> getTargets(String id, Actor<?> source) {
		if (targeter == null) return List.of(source);

		List<Actor<?>> out = new ArrayList<>();
		try {
			Utils.exec(id, targeter, Map.of(
					"ctx", new SkillContext(source, null)
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to load targets {}", id, e);
		}

		return out;
	}

	public CpuRule canCpuUse(String id, Actor<?> source, Actor<?> target) {
		if (cpuRule == null) return CpuRule.ANY;

		try {
			Object out = Utils.exec(id, cpuRule, Map.of(
					"ctx", new SkillContext(source, target)
			));

			if (out instanceof Boolean b) {
				return b ? CpuRule.FORCE : CpuRule.PREVENT;
			}
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to check CPU rule {}", id, e);
		}

		return CpuRule.ANY;
	}

	public String getEffect() {
		return effect;
	}
}
