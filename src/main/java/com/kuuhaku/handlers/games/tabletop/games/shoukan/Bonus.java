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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Flag;
import com.kuuhaku.utils.json.JSONObject;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.IntStream;

public class Bonus implements Cloneable {
	private JSONObject specialData;
	private EnumSet<Flag> flags;
	private EnumSet<Flag> pFlags;
	private int[] atk = new int[6];
	private int[] def = new int[6];
	private int[] ddg = new int[6];
	private int[] blk = new int[6];
	private int mana = 0;
	private int blood = 0;
	private String write = null;
	private Charm charm = null;

	public Bonus(JSONObject specialData, EnumSet<Flag> flags, EnumSet<Flag> pFlags, int atk, int def, int ddg, int blk, int mana, int blood, String write, Charm charm) {
		this.specialData = specialData;
		this.flags = flags;
		this.pFlags = pFlags;
		this.atk[0] = atk;
		this.def[0] = def;
		this.ddg[0] = ddg;
		this.blk[0] = blk;
		this.mana = mana;
		this.blood = blood;
		this.write = write;
		this.charm = charm;
	}

	public Bonus(JSONObject specialData) {
		this.specialData = new JSONObject(specialData.toString());
		this.flags = EnumSet.noneOf(Flag.class);
		this.pFlags = EnumSet.noneOf(Flag.class);
	}

	public Bonus() {
		this.specialData = new JSONObject();
		this.flags = EnumSet.noneOf(Flag.class);
		this.pFlags = EnumSet.noneOf(Flag.class);
	}

	public JSONObject getSpecialData() {
		return specialData;
	}

	public void putProp(String key, Object value) {
		specialData.put(key, value);
	}

	public EnumSet<Flag> getFlags() {
		return flags;
	}

	public boolean hasFlag(Flag flag) {
		return flags.contains(flag) || pFlags.contains(flag);
	}

	public boolean popFlag(Flag flag) {
		return flags.remove(flag) || pFlags.contains(flag);
	}

	public boolean popFlag(Champion op, Flag flag) {
		if (op.getBonus().hasFlag(Flag.NOEFFECT)) return false;

		return flags.remove(flag) || pFlags.contains(flag);
	}

	public void setFlag(Flag flag, boolean on) {
		if (on) flags.add(flag);
		else flags.remove(flag);
	}

	public EnumSet<Flag> getPFlags() {
		return pFlags;
	}

	public void setPFlag(Flag flag, boolean on) {
		if (on) pFlags.add(flag);
		else pFlags.remove(flag);
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

	public int getDodge() {
		return IntStream.of(ddg).sum();
	}

	public void setDodge(int ddg) {
		this.ddg[0] = ddg;
	}

	public void setDodge(int index, int ddg) {
		this.ddg[index + 1] = ddg;
	}

	public void addDodge(int ddg) {
		this.ddg[0] += ddg;
	}

	public void addDodge(int index, int ddg) {
		this.ddg[index + 1] += ddg;
	}

	public void removeDodge(int ddg) {
		this.ddg[0] -= ddg;
	}

	public void removeDodge(int index, int ddg) {
		this.ddg[index + 1] -= ddg;
	}

	public int getBlock() {
		return IntStream.of(blk).sum();
	}

	public void setBlock(int blk) {
		this.blk[0] = blk;
	}

	public void setBlock(int index, int blk) {
		this.blk[index + 1] = blk;
	}

	public void addBlock(int blk) {
		this.blk[0] += blk;
	}

	public void addBlock(int index, int blk) {
		this.blk[index + 1] += blk;
	}

	public void removeBlock(int blk) {
		this.blk[0] -= blk;
	}

	public void removeBlock(int index, int blk) {
		this.blk[index + 1] -= blk;
	}

	public int getMana() {
		return mana;
	}

	public void setMana(int mana) {
		this.mana = mana;
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

	public void setBlood(int blood) {
		this.blood = blood;
	}

	public void addBlood(int blood) {
		this.blood += blood;
	}

	public void removeBlood(int blood) {
		this.blood -= blood;
	}

	public String getWrite() {
		return write;
	}

	public void setWrite(String write) {
		this.write = write;
	}

	public Charm getCharm() {
		return charm;
	}

	public void setCharm(Charm charm) {
		this.charm = charm;
	}

	@Override
	public Bonus clone() {
		try {
			Bonus b = (Bonus) super.clone();
			b.specialData = new JSONObject(specialData);
			b.flags = EnumSet.copyOf(flags);
			b.pFlags = EnumSet.copyOf(pFlags);
			b.atk = Arrays.copyOf(atk, 6);
			b.def = Arrays.copyOf(def, 6);
			b.ddg = Arrays.copyOf(ddg, 6);
			b.blk = Arrays.copyOf(blk, 6);

			return b;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
