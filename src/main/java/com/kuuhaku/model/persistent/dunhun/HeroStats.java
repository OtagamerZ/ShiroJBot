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

import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.util.Bit32;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class HeroStats {
	@Column(name = "hp", nullable = false)
	private int hp;

	@Column(name = "xp", nullable = false)
	private int xp;

	@Column(name = "attributes", nullable = false)
	private int attributes;
	/*
	0xFF FF FF FF
	  └┤ └┤ └┤ └┴ strength
	   │  │  └ dexterity
	   │  └ wisdom
	   └ vitality
	 */

	@Column(name = "image_hash", nullable = false)
	private String imageHash;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "equipment", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject equipment;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "inventory", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject inventory;

	public int getHp() {
		return hp;
	}

	public int getMaxHp() {
		return 100 + getVitality() * 10;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}

	public int getXp() {
		return xp;
	}

	public void addXp(int xp) {
		this.xp += xp;
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

	public JSONObject getEquipment() {
		return equipment;
	}

	public JSONObject getInventory() {
		return inventory;
	}
}
