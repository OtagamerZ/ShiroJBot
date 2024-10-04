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
import java.util.function.Predicate;

public class MonsterModifiers {
	private final CumValue attack = CumValue.flat();
	private final CumValue attackMult = CumValue.flat();
	private final CumValue defense = CumValue.flat();
	private final CumValue defenseMult = CumValue.flat();
	private final CumValue maxHp = CumValue.flat();
	private final CumValue hpMult = CumValue.flat();
	private final CumValue maxAp = CumValue.flat();
	private final CumValue dodge = CumValue.flat();
	private final CumValue parry = CumValue.flat();
	private final CumValue initiative = CumValue.flat();

	private transient Field[] fieldCache = null;

	public int getAttack() {
		return (int) attack.get();
	}

	public void addAttack(int value) {
		addAttack(value, -1);
	}

	public void addAttack(int value, int expiration) {
		attack.set(null, value, expiration);
	}

	public double getAttackMult() {
		return attackMult.get();
	}

	public void addAttackMult(float mult) {
		addAttackMult(mult, -1);
	}

	public void addAttackMult(float mult, int expiration) {
		attackMult.set(null, mult, expiration);
	}

	public int getDefense() {
		return (int) defense.get();
	}

	public void addDefense(int value) {
		addDefense(value, -1);
	}

	public void addDefense(int value, int expiration) {
		defense.set(null, value, expiration);
	}

	public double getDefenseMult() {
		return defenseMult.get();
	}

	public void addDefenseMult(float mult) {
		addDefenseMult(mult, -1);
	}

	public void addDefenseMult(float mult, int expiration) {
		defenseMult.set(null, mult, expiration);
	}

	public int getMaxHp() {
		return (int) maxHp.get();
	}

	public void addMaxHp(int value) {
		addMaxHp(value, -1);
	}

	public void addMaxHp(int value, int expiration) {
		maxHp.set(null, value, expiration);
	}

	public double getHpMult() {
		return hpMult.get();
	}

	public void addHpMult(float mult) {
		addHpMult(mult, -1);
	}

	public void addHpMult(float mult, int expiration) {
		hpMult.set(null, mult, expiration);
	}

	public int getMaxAp() {
		return (int) maxAp.get();
	}

	public void addMaxAp(int value) {
		addMaxAp(value, -1);
	}

	public void addMaxAp(int value, int expiration) {
		maxAp.set(null, value, expiration);
	}

	public int getDodge() {
		return (int) dodge.get();
	}

	public void addDodge(int value) {
		addDodge(value, -1);
	}

	public void addDodge(int value, int expiration) {
		dodge.set(null, value, expiration);
	}

	public int getParry() {
		return (int) parry.get();
	}

	public void addParry(int value) {
		addParry(value, -1);
	}

	public void addParry(int value, int expiration) {
		parry.set(null, value, expiration);
	}

	public int getInitiative() {
		return (int) initiative.get();
	}

	public void addInitiative(int value) {
		addInitiative(value, -1);
	}

	public void addInitiative(int value, int expiration) {
		initiative.set(null, value, expiration);
	}

	public void expireMods() {
		Predicate<ValueMod> check = mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		};

		removeIf(check);
	}

	public void clear() {
		removeIf(o -> true);
	}

	public void removeIf(Predicate<ValueMod> check) {
		if (fieldCache == null) {
			fieldCache = getClass().getDeclaredFields();
		}

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
