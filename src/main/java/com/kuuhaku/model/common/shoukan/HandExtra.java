/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.util.Calc;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;

public class HandExtra {
	private final CumValue healMult = new CumValue();
	private final CumValue damageMult = new CumValue();
	private final CumValue regenMult = new CumValue();
	private final CumValue degenMult = new CumValue();

	private final Field[] fieldCache = getClass().getDeclaredFields();

	public CumValue getHealMult() {
		return healMult;
	}

	public CumValue getDamageMult() {
		return damageMult;
	}

	public CumValue getRegenMult() {
		return regenMult;
	}

	public CumValue getDegenMult() {
		return degenMult;
	}

	public void expireMods() {
		removeIf(mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		});
	}

	public void removeIf(Predicate<ValueMod> check) {
		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof CumValue cv) {
					cv.values().removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	private double sum(Set<ValueMod> mods) {
		double out = 0;
		for (ValueMod mod : mods) {
			out += mod.getValue();
		}

		return Calc.round(out, 2);
	}
}
