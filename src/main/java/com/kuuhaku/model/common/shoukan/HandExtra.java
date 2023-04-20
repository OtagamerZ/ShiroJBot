/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.interfaces.shoukan.Drawable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class HandExtra {
	private final Set<ValueMod> healMult = new HashSet<>();
	private final Set<ValueMod> damageMult = new HashSet<>();

	private transient Field[] fieldCache = null;

	public int getHealMult() {
		return 1 + sum(healMult);
	}

	public void setHealing(double mult) {
		for (ValueMod mod : this.healMult) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + mult);
				return;
			}
		}

		this.healMult.add(new PermMod(mult));
	}

	public void setHealing(Drawable<?> source, double mult) {
		ValueMod mod = new ValueMod(source, mult);
		this.healMult.remove(mod);
		this.healMult.add(mod);
	}

	public void setHealing(Drawable<?> source, double mult, int expiration) {
		ValueMod mod = new ValueMod(source, mult, expiration);
		this.healMult.remove(mod);
		this.healMult.add(mod);
	}

	public int getDamageMult() {
		return 1 + sum(damageMult);
	}

	public void setDamageMult(double mult) {
		for (ValueMod mod : this.damageMult) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + mult);
				return;
			}
		}

		this.damageMult.add(new PermMod(mult));
	}

	public void setDamageMult(Drawable<?> source, double mult) {
		ValueMod mod = new ValueMod(source, mult);
		this.damageMult.remove(mod);
		this.damageMult.add(mod);
	}

	public void setDamageMult(Drawable<?> source, double mult, int expiration) {
		ValueMod mod = new ValueMod(source, mult, expiration);
		this.damageMult.remove(mod);
		this.damageMult.add(mod);
	}

	public void expireMods() {
		Predicate<ValueMod> check = mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		};

		removeExpired(check);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void removeExpired(Predicate<ValueMod> check) {
		if (fieldCache == null) {
			fieldCache = getClass().getDeclaredFields();
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof HashSet s) {
					s.removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	private int sum(Set<ValueMod> mods) {
		double out = 0;
		for (ValueMod mod : mods) {
			out += mod.getValue();
		}

		return (int) Math.round(out);
	}
}
