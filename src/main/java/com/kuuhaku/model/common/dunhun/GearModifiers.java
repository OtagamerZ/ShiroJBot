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
	private float attackMult = 1;
	private int defense;
	private float defenseMult = 1;
	private float critical;
	private float criticalMult = 1;

	private final Set<String> addedTags = new HashSet<>();
	private float affixMult = 1;

	public float getAttack() {
		return attack;
	}

	public void addAttack(int value) {
		attack += value;
	}

	public float getAttackMult() {
		return attackMult;
	}

	public void addAttackMult(float mult) {
		attackMult += mult;
	}

	public float getDefense() {
		return defense;
	}

	public void addDefense(int value) {
		defense += value;
	}

	public float getDefenseMult() {
		return defenseMult;
	}

	public void addDefenseMult(float mult) {
		defenseMult += mult;
	}

	public float getCritical() {
		return critical;
	}

	public void addCritical(float mult) {
		critical += mult;
	}

	public float getCriticalMult() {
		return criticalMult;
	}

	public void addCriticalMult(float mult) {
		criticalMult += mult;
	}

	public Set<String> getAddedTags() {
		return addedTags;
	}

	public float getAffixMult() {
		return affixMult;
	}

	public void addAffixMult(float mult) {
		this.affixMult += mult;
	}

	public void reset() {
		attack = defense = 0;
		critical = 0;
		attackMult = defenseMult = criticalMult = affixMult = 1;
		addedTags.clear();
	}
}
