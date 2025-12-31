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
import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.context.SkillContext;
import com.kuuhaku.model.enums.dunhun.CpuRule;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.intellij.lang.annotations.Language;

import java.util.Map;

@Embeddable
public class SkillStats extends UsableStats {
	@Column(name = "reservation", nullable = false)
	private int reservation;

	@Column(name = "cooldown", nullable = false)
	private int cooldown;

	@Column(name = "efficiency", nullable = false)
	private double efficiency;

	@Column(name = "critical", nullable = false)
	private double critical;

	@Column(name = "spell", nullable = false)
	private boolean spell;

	@Language("Groovy")
	@Column(name = "cpu_rule", columnDefinition = "TEXT")
	private String cpuRule;

	public SkillStats() {
	}

	public SkillStats(int cost, int cooldown, double efficiency, double critical, boolean spell) {
		super(cost);
		this.cooldown = cooldown;
		this.efficiency = efficiency;
		this.critical = critical;
		this.spell = spell;
	}

	public int getReservation() {
		return reservation;
	}

	public int getCooldown() {
		return cooldown;
	}

	public double getEfficiency(int level) {
		return efficiency * (1 + Calc.clamp(level, 0, 100) * 0.003);
	}

	public double getCritical() {
		return critical;
	}

	public boolean isSpell() {
		return spell;
	}

	public CpuRule canCpuUse(Usable usable, Actor<?> source, Actor<?> target) {
		if (cpuRule == null) return CpuRule.ANY;

		try {
			Object out = Utils.exec(usable.getId(), cpuRule, Map.of(
					"ctx", new SkillContext(source, target, usable)
			));

			if (out instanceof Boolean b) {
				return b ? CpuRule.FORCE : CpuRule.PREVENT;
			}
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to check CPU rule {}", usable.getId(), e);
		}

		return CpuRule.ANY;
	}

	public SkillStats copyWith(double efficiency, double critical) {
		SkillStats clone = (SkillStats) super.copy();
		clone.efficiency = efficiency;
		clone.critical = critical;

		return clone;
	}
}
