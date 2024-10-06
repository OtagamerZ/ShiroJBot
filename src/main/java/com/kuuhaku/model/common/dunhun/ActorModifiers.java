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
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.records.Attributes;
import com.kuuhaku.util.Bit32;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

public class ActorModifiers {
	private final CumValue maxHp = CumValue.flat();
	private final CumValue maxAp = CumValue.flat();
	private final CumValue initiative = CumValue.flat();
	private final CumValue hpMult = CumValue.mult();

	private final Map<String, Integer> cooldowns = new HashMap<>();
	private final CumValue attributes = CumValue.flat();
	/*
	0xFF FF FF FF
	  └┤ └┤ └┤ └┴ strength
	   │  │  └ dexterity
	   │  └ wisdom
	   └ vitality
	 */

	private Field[] fieldCache = null;

	public CumValue getMaxHp() {
		return maxHp;
	}

	public CumValue getHpMult() {
		return hpMult;
	}

	public CumValue getMaxAp() {
		return maxAp;
	}

	public CumValue getInitiative() {
		return initiative;
	}

	public int getStrength() {
		return Bit32.get((int) attributes.get(), 0, 8);
	}

	public void addStrength(int value) {
		addStrength(value, -1);
	}

	public void addStrength(int value, int expiration) {
		attributes.set(null,
				Bit32.set((int) attributes.get(), 0, getStrength() + value, 8),
				expiration
		);
	}

	public int getDexterity() {
		return Bit32.get((int) attributes.get(), 0, 8);
	}

	public void addDexterity(int value) {
		addDexterity(value, -1);
	}

	public void addDexterity(int value, int expiration) {
		attributes.set(null,
				Bit32.set((int) attributes.get(), 1, getDexterity() + value, 8),
				expiration
		);
	}

	public int getWisdom() {
		return Bit32.get((int) attributes.get(), 2, 8);
	}

	public void addWisdom(int value) {
		addWisdom(value, -1);
	}

	public void addWisdom(int value, int expiration) {
		attributes.set(null,
				Bit32.set((int) attributes.get(), 2, getWisdom() + value, 8),
				expiration
		);
	}

	public int getVitality() {
		return Bit32.get((int) attributes.get(), 3, 8);
	}

	public void addVitality(int value) {
		addVitality(value, -1);
	}

	public void addVitality(int value, int expiration) {
		attributes.set(null,
				Bit32.set((int) attributes.get(), 3, getVitality() + value, 8),
				expiration
		);
	}

	public Attributes getAttributes() {
		return new Attributes(getStrength(), getDexterity(), getWisdom(), getVitality());
	}

	public void setCooldown(Skill s, int time) {
		cooldowns.put(s.getId(), time);
	}

	public boolean isCoolingDown(Skill s) {
		return cooldowns.getOrDefault(s.getId(), 0) > 0;
	}

	public Map<String, Integer> getCooldowns() {
		return cooldowns;
	}

	public void expireMods() {
		Iterator<Map.Entry<String, Integer>> it = cooldowns.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Integer> entry = it.next();

			int cd = entry.getValue() - 1;
			if (cd <= 0) it.remove();
			else entry.setValue(cd);
		}

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
