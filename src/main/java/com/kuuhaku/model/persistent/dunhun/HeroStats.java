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
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.NavigableMap;
import java.util.TreeMap;

@Embeddable
public class HeroStats implements Serializable {
	private static final NavigableMap<Integer, Integer> xpTable = new TreeMap<>();

	static {
		for (int i = 1; i <= 1000; i++) {
			xpTable.put((int) (Math.pow(i, 2.5) * 10), i);
		}
	}

	@Column(name = "xp", nullable = false)
	private int xp;

	@Enumerated(EnumType.STRING)
	@Column(name = "race", nullable = false)
	private Race race;

	@Column(name = "evil", nullable = false)
	private boolean evil;

	@Embedded
	private Attributes attributes = new Attributes();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "skills", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray equippedSkills = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "unlocked_skills", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray unlockedSkills = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "consumables", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject consumables = new JSONObject();

	public int getLevel() {
		return xpTable.floorEntry(xp).getValue() + 1;
	}

	public int getPointsLeft() {
		return (4 + Math.min(100, getLevel())) - attributes.count() - unlockedSkills.size();
	}

	public int getXpToCurrent() {
		return xpTable.floorKey(xp);
	}

	public int getXpToNext() {
		return xpTable.ceilingKey(xp);
	}

	public int getLosableXp() {
		return getXpToNext() - getXpToCurrent();
	}

	public int getXp() {
		return xp;
	}

	public void addXp(int xp) {
		this.xp += xp;
	}

	public void loseXp(int xp) {
		this.xp = Math.max(getXpToCurrent(), this.xp - xp);
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public JSONArray getSkills() {
		return equippedSkills;
	}

	public void setSkills(JSONArray equippedSkills) {
		this.equippedSkills = equippedSkills;
	}

	public JSONArray getUnlockedSkills() {
		return unlockedSkills;
	}

	public void setUnlockedSkills(JSONArray unlockedSkills) {
		this.unlockedSkills = unlockedSkills;
	}

	public JSONObject getConsumables() {
		return consumables;
	}
}
