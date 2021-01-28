/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import org.json.JSONObject;

public class Bonus implements Cloneable {
	private int atk = 0;
	private int def = 0;
	private final JSONObject specialData;

	public Bonus(int atk, int def, JSONObject specialData) {
		this.atk = atk;
		this.def = def;
		this.specialData = new JSONObject(specialData.toString());
	}

	public Bonus(JSONObject specialData) {
		this.specialData = new JSONObject(specialData.toString());
	}

	public Bonus() {
		this.specialData = new JSONObject();
	}

	public int getAtk() {
		return atk;
	}

	public void setAtk(int atk) {
		this.atk = atk;
	}

	public int getDef() {
		return def;
	}

	public void setDef(int def) {
		this.def = def;
	}

	public void addAtk(int atk) {
		this.atk += atk;
	}

	public void removeAtk(int atk) {
		this.atk -= atk;
	}

	public void addDef(int def) {
		this.def += def;
	}

	public void removeDef(int def) {
		this.def -= def;
	}

	public JSONObject getSpecialData() {
		return specialData;
	}

	public Bonus copy() {
		try {
			Bonus b = (Bonus) clone();

			return new Bonus(
					b.getAtk(),
					b.getDef(),
					b.getSpecialData()
			);
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
