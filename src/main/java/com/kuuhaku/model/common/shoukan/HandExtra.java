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
import com.kuuhaku.model.records.shoukan.ValueMod;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class HandExtra {
	private final Set<ValueMod> healing = new HashSet<>();
	private final Set<ValueMod> damage = new HashSet<>();

	public int getHealing() {
		return 1 + sum(healing);
	}

	public void setHealing(Drawable<?> source, int healing) {
		ValueMod mod = new ValueMod(source, healing);
		this.healing.remove(mod);
		this.healing.add(mod);
	}

	public void setHealing(Drawable<?> source, int healing, int expiration) {
		ValueMod mod = new ValueMod(source, healing, expiration);
		this.healing.remove(mod);
		this.healing.add(mod);
	}

	public int getDamage() {
		return 1 + sum(damage);
	}

	public void setDamage(Drawable<?> source, int damage) {
		ValueMod mod = new ValueMod(source, damage);
		this.damage.remove(mod);
		this.damage.add(mod);
	}

	public void setDamage(Drawable<?> source, int damage, int expiration) {
		ValueMod mod = new ValueMod(source, damage, expiration);
		this.damage.remove(mod);
		this.damage.add(mod);
	}

	public void expireMods() {
		Predicate<ValueMod> check = mod -> {
			if (mod.expiration() != null) {
				return mod.expiration().decrementAndGet() <= 0;
			}

			return false;
		};

		healing.removeIf(check);
		damage.removeIf(check);
	}

	private int sum(Set<ValueMod> mods) {
		double out = 0;
		for (ValueMod mod : mods) {
			out += mod.value();
		}

		return (int) Math.round(out);
	}
}
