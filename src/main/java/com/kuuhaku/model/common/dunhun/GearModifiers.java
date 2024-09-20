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
	private float attackMult = 1;
	private float defenseMult = 1;
	private float criticalMult = 1;

	private final Set<String> addedTags = new HashSet<>();

	public float getAttackMult() {
		return attackMult;
	}

	public void addAttackMult(float mult) {
		attackMult += mult;
	}

	public float getDefenseMult() {
		return defenseMult;
	}

	public void addDefenseMult(float mult) {
		defenseMult += mult;
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

	public void reset() {
		attackMult = defenseMult = criticalMult;
	}
}
