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

import com.kuuhaku.utils.Helper;

import javax.persistence.Embeddable;

@Embeddable
public class Attributes {
	private int str = 0;
	private int res = 0;
	private int agi = 0;
	private int wis = 0;
	private int con = 0;

	public int getStr() {
		return str;
	}

	public void setStr(int str) {
		this.str = Math.max(0, str);
	}

	public void addStr() {
		this.str += 1;
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

	public int getAgi() {
		return agi;
	}

	public void setAgi(int agi) {
		this.agi = Math.max(0, agi);
	}

	public void addAgi() {
		this.agi += 1;
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

	public int getCon() {
		return con;
	}

	public void setCon(int con) {
		this.con = Math.max(0, con);
	}

	public void addCon() {
		this.con += 1;
	}

	public int calcMaxHp() {
		return (int) Helper.roundTrunc(1000 + 3000 * (1 - Math.exp(-0.05 * con + -0.01 * str + -0.015 * str)), 5);
	}

	public int calcMp() {
		return (int) (1 + Math.max(0,
				str * 0.2
				+ res * 0.1
				+ agi * 0.02
				+ wis * -0.15
				+ con * 0.05
		));
	}

	public int calcAtk() {
		return (int) Helper.roundTrunc(100 + 3000 * (1 - Math.exp(-0.02 * str + -0.005 * agi)), 25);
	}

	public int calcDef() {
		return (int) Helper.roundTrunc(100 + 2500 * (1 - Math.exp(-0.03 * res + -0.0075 * agi)), 25);
	}

	public int calcDodge() {
		return (int) Helper.roundTrunc(100 * (1 - Math.exp(-0.05 * agi + 0.025 * con)), 5);
	}

	public int getUsedPoints() {
		return str + res + agi + wis + con;
	}
}