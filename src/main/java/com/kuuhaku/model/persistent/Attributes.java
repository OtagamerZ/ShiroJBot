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

package com.kuuhaku.model.persistent;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.utils.Helper;

import javax.persistence.Embeddable;
import java.util.Set;

@Embeddable
public class Attributes {
	private int str = 0;
	private int res = 0;
	private int agi = 0;
	private int wis = 0;
	private int con = 0;

	public Attributes() {

	}

	public Attributes(Integer[] stats) {
		str = stats[0];
		res = stats[1];
		agi = stats[2];
		wis = stats[3];
		con = stats[4];
	}

	public int getStr() {
		return str;
	}

	public void setStr(int str) {
		this.str = Math.max(0, str);
	}

	public void addStr() {
		this.str += 1;
	}

	public void addStr(int value) {
		this.str += value;
	}

	public int getRes() {
		return res;
	}

	public void setRes(int res) {
		this.res = Math.max(0, res);
	}

	public void addRes() {
		this.res += 1;
	}

	public void addRes(int value) {
		this.res += value;
	}

	public int getAgi() {
		return agi;
	}

	public void setAgi(int agi) {
		this.agi = Math.max(0, agi);
	}

	public void addAgi() {
		this.agi += 1;
	}

	public void addAgi(int value) {
		this.agi += value;
	}

	public int getWis() {
		return wis;
	}

	public void setWis(int wis) {
		this.wis = Math.max(0, wis);
	}

	public void addWis() {
		this.wis += 1;
	}

	public void addWis(int value) {
		this.wis += value;
	}

	public int getCon() {
		return con;
	}

	public void setCon(int con) {
		this.con = Math.max(0, con);
	}

	public void addCon() {
		this.con += 1;
	}

	public void addCon(int value) {
		this.con += value;
	}

	public int calcMaxHp(Set<Perk> perks) {
		double hpModif = 1;
		for (Perk perk : perks) {
			hpModif *= switch (perk) {
				case VANGUARD -> 1.33;
				case NIMBLE, VAMPIRE -> 0.75;
				case MANALESS -> 0.5;
				default -> 1;
			};
		}

		return (int) Helper.roundTrunc((1000 + 3000 * (1 - Math.exp(-0.045 * con + -0.01 * str + -0.015 * res))) * hpModif, 5);
	}

	public int calcMaxEnergy() {
		return (int) Math.round(10 * (1 - Math.exp(-0.06 * res + -0.03 * con)));
	}

	public int calcMp(Champion ref) {
		return (int) (1 + (ref == null ? 0 : ref.getMana() * 0.75) + Math.max(0,
				str * 0.275
				+ res * 0.15
				+ agi * 0.0175
				+ wis * -0.15
				+ con * 0.075
		));
	}

	public int calcAtk() {
		return (int) Helper.roundTrunc(100 + 2750 * (1 - Math.exp(-0.017 * str + -0.005 * agi)), 25);
	}

	public int calcDef() {
		return (int) Helper.roundTrunc(100 + 2500 * (1 - Math.exp(-0.022 * res + -0.0075 * agi)), 25);
	}

	public int calcDodge() {
		return (int) Math.round(25 * (1 - Math.exp(-0.05 * agi + 0.025 * con)));
	}

	public Integer[] getStats() {
		return new Integer[]{str, res, agi, wis, con};
	}

	public int getUsedPoints() {
		return str + res + agi + wis + con;
	}
}