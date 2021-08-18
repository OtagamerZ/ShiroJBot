/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.utils.JSONObject;

import java.util.stream.IntStream;

public class Bonus implements Cloneable {
	private final JSONObject specialData;
	private final int[] atk = new int[6];
	private final int[] def = new int[6];
	private int mana = 0;
	private int blood = 0;

	public Bonus(JSONObject specialData, int atk, int def, int mana, int blood) {
		this.specialData = new JSONObject(specialData.toString());
		this.atk[0] = atk;
		this.def[0] = def;
		this.mana = mana;
		this.blood = blood;
	}

	public Bonus(JSONObject specialData) {
		this.specialData = new JSONObject(specialData.toString());
	}

	public Bonus() {
		this.specialData = new JSONObject();
	}

	public JSONObject getSpecialData() {
		return specialData;
	}

	public int getAtk() {
		return IntStream.of(atk).sum();
	}

	public void setAtk(int atk) {
		this.atk[0] = atk;
	}

	public void setAtk(int index, int atk) {
		this.atk[index + 1] = atk;
	}

	public void addAtk(int atk) {
		this.atk[0] += atk;
	}

	public void addAtk(int index, int atk) {
		this.atk[index + 1] += atk;
	}

	public void removeAtk(int atk) {
		this.atk[0] -= atk;
	}

	public void removeAtk(int index, int atk) {
		this.atk[index + 1] -= atk;
	}

	public int getDef() {
		return IntStream.of(def).sum();
	}

	public void setDef(int def) {
		this.def[0] = def;
	}

	public void setDef(int index, int def) {
		this.def[index + 1] = def;
	}

	public void addDef(int def) {
		this.def[0] += def;
	}

	public void addDef(int index, int def) {
		this.def[index + 1] += def;
	}

	public void removeDef(int def) {
		this.def[0] -= def;
	}

	public void removeDef(int index, int def) {
		this.def[index + 1] -= def;
	}

	public int getMana() {
		return mana;
	}

	public void addMana(int mana) {
		this.mana += mana;
	}

	public void removeMana(int mana) {
		this.mana -= mana;
	}

	public int getBlood() {
		return blood;
	}

	public void addBlood(int blood) {
		this.blood += blood;
	}

	public void removeBlood(int blood) {
		this.blood -= blood;
	}

	public Bonus copy() {
		try {
			Bonus b = (Bonus) clone();

			return new Bonus(
					b.getSpecialData(),
					b.getAtk(),
					b.getDef(),
					b.getMana(),
					b.getBlood()
			);
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
