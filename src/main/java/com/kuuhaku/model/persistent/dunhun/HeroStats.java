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

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.records.Attributes;
import com.kuuhaku.util.Bit32;
import com.kuuhaku.util.Calc;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class HeroStats {
	@Column(name = "xp", nullable = false)
	private int xp;

	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race race;

	@Column(name = "attributes", nullable = false)
	private int attributes;
	/*
	0xFF FF FF FF
	  └┤ └┤ └┤ └┴ strength
	   │  │  └ dexterity
	   │  └ wisdom
	   └ vitality
	 */

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "skills", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray skills = new JSONArray();

	public int getLevel() {
		return 1 + Calc.round(Math.pow(xp / 10d, 1 / 1.5));
	}

	public int getXpToNext() {
		return (int) (Math.pow(getLevel(), 1.5) * 10);
	}

	public int getXp() {
		return xp;
	}

	public void addXp(int xp) {
		this.xp += xp;
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public int getStrength() {
		return Bit32.get(attributes, 0, 8);
	}

	public void setStrength(int value) {
		attributes = Bit32.set(attributes, 0, value, 8);
	}

	public int getDexterity() {
		return Bit32.get(attributes, 0, 8);
	}

	public void setDexterity(int value) {
		attributes = Bit32.set(attributes, 1, value, 8);
	}

	public int getWisdom() {
		return Bit32.get(attributes, 2, 8);
	}

	public void setWisdom(int value) {
		attributes = Bit32.set(attributes, 2, value, 8);
	}

	public int getVitality() {
		return Bit32.get(attributes, 3, 8);
	}

	public void setVitality(int value) {
		attributes = Bit32.set(attributes, 3, value, 8);
	}

	public Attributes getAttributes() {
		return new Attributes(getStrength(), getDexterity(), getWisdom(), getVitality());
	}

	public JSONArray getSkills() {
		return skills;
	}
}
