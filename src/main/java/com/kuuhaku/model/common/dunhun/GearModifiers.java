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

package com.kuuhaku.model.common.dunhun;

import java.util.HashSet;
import java.util.Set;

public class GearModifiers {
	private int attack;
	private double attackMult = 1;
	private int defense;
	private double defenseMult = 1;
	private double critical;
	private double criticalMult = 1;

	private final Set<String> addedTags = new HashSet<>();

	public double getAttack() {
		return attack;
	}

	public void addAttack(int value) {
		attack += value;
	}

	public double getAttackMult() {
		return attackMult;
	}

	public void addAttackMult(double mult) {
		attackMult += mult;
	}

	public double getDefense() {
		return defense;
	}

	public void addDefense(int value) {
		defense += value;
	}

	public double getDefenseMult() {
		return defenseMult;
	}

	public void addDefenseMult(double mult) {
		defenseMult += mult;
	}

	public double getCritical() {
		return critical;
	}

	public void addCritical(double mult) {
		critical += mult;
	}

	public double getCriticalMult() {
		return criticalMult;
	}

	public void addCriticalMult(double mult) {
		criticalMult += mult;
	}

	public Set<String> getAddedTags() {
		return addedTags;
	}

	public void reset() {
		attack = defense = 0;
		critical = 0;
		attackMult = defenseMult = criticalMult = 1;
		addedTags.clear();
	}
}
