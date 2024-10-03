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

import com.kuuhaku.model.records.Attributes;
import com.kuuhaku.util.Bit32;

import java.util.HashSet;
import java.util.Set;

public class HeroModifiers {
	private int maxHp;
	private int maxAp;
	private int initiative;
	private int attributes;
	/*
	0xFF FF FF FF
	  └┤ └┤ └┤ └┴ strength
	   │  │  └ dexterity
	   │  └ wisdom
	   └ vitality
	 */

	private final Set<String> skills = new HashSet<>();
	private final Set<String> effects = new HashSet<>();

	public int getMaxHp() {
		return maxHp;
	}

	public void addMaxHp(int value) {
		maxHp += value;
	}

	public int getMaxAp() {
		return maxAp;
	}

	public void addMaxAp(int value) {
		maxAp += value;
	}

	public int getInitiative() {
		return initiative;
	}

	public void addInitiative(int value) {
		initiative += value;
	}

	public int getStrength() {
		return Bit32.get(attributes, 0, 8);
	}

	public void addStrength(int value) {
		attributes = Bit32.set(attributes, 0, getStrength() + value, 8);
	}

	public int getDexterity() {
		return Bit32.get(attributes, 0, 8);
	}

	public void addDexterity(int value) {
		attributes = Bit32.set(attributes, 1, getDexterity() + value, 8);
	}

	public int getWisdom() {
		return Bit32.get(attributes, 2, 8);
	}

	public void addWisdom(int value) {
		attributes = Bit32.set(attributes, 2, getWisdom() + value, 8);
	}

	public int getVitality() {
		return Bit32.get(attributes, 3, 8);
	}

	public void addVitality(int value) {
		attributes = Bit32.set(attributes, 3, getVitality() + value, 8);
	}

	public Attributes getAttributes() {
		return new Attributes(getStrength(), getDexterity(), getWisdom(), getVitality());
	}

	public Set<String> getSkills() {
		return skills;
	}

	public Set<String> getEffects() {
		return effects;
	}

	public void reset() {
		maxHp = maxAp = initiative = attributes = 0;
		skills.clear();
		effects.clear();
	}
}
