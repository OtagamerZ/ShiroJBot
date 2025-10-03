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

import com.kuuhaku.model.common.shoukan.CumValue;
import com.kuuhaku.model.common.shoukan.ValueMod;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class GearModifiers {
	private final CumValue attack = CumValue.flat();
	private final CumValue attackMult = CumValue.flat();
	private final CumValue defense = CumValue.flat();
	private final CumValue defenseMult = CumValue.flat();
	private final CumValue critical = CumValue.flat();
	private final CumValue criticalMult = CumValue.flat();
	private final CumValue prefixes = CumValue.flat();
	private final CumValue suffixes = CumValue.flat();

	private final Set<String> addedTags = new HashSet<>();

	private final Field[] fieldCache = getClass().getDeclaredFields();

	public CumValue getAttack() {
		return attack;
	}

	public CumValue getAttackMult() {
		return attackMult;
	}

	public CumValue getDefense() {
		return defense;
	}

	public CumValue getDefenseMult() {
		return defenseMult;
	}

	public CumValue getCritical() {
		return critical;
	}

	public CumValue getCriticalMult() {
		return criticalMult;
	}

	public CumValue getPrefixes() {
		return prefixes;
	}

	public CumValue getSuffixes() {
		return suffixes;
	}

	public Set<String> getAddedTags() {
		return addedTags;
	}

	public void expireMods() {
		removeIf(mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		});
	}

	public void clear() {
		removeIf(o -> true);
	}

	public void removeIf(Predicate<ValueMod> check) {
		addedTags.clear();

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof CumValue cv) {
					cv.values().removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}
}
