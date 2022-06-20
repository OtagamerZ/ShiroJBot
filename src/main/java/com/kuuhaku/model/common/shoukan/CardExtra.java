/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.id.LocalizedDescId;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.LocalizedDescription;
import com.kuuhaku.model.records.shoukan.AttrMod;
import com.kuuhaku.utils.Calc;
import com.kuuhaku.utils.json.JSONObject;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class CardExtra {
	private Set<AttrMod> mana = new HashSet<>();
	private Set<AttrMod> blood = new HashSet<>();

	private Set<AttrMod> atk = new HashSet<>();
	private Set<AttrMod> def = new HashSet<>();

	private Set<AttrMod> dodge = new HashSet<>();
	private Set<AttrMod> block = new HashSet<>();

	private Set<AttrMod> power = new HashSet<>();

	private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
	private EnumSet<Flag> permFlags = EnumSet.noneOf(Flag.class);

	private JSONObject data = new JSONObject();

	private Race race = null;
	private Card vanity = null;

	private String write = "";
	private String description = null;
	private String effect = null;

	public int getMana() {
		return (int) sum(mana);
	}

	public void setMana(Drawable source, int mana) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), mana);
		this.mana.remove(mod);
		this.mana.add(mod);
	}

	public void setMana(Drawable source, int mana, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), mana, expiration);
		this.mana.remove(mod);
		this.mana.add(mod);
	}

	public int getBlood() {
		return (int) sum(blood);
	}

	public void setBlood(Drawable source, int blood) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), blood);
		this.blood.remove(mod);
		this.blood.add(mod);
	}

	public void setBlood(Drawable source, int blood, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), blood, expiration);
		this.blood.remove(mod);
		this.blood.add(mod);
	}

	public int getAtk() {
		return (int) sum(atk);
	}

	public void setAtk(Drawable source, int atk) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), atk);
		this.atk.remove(mod);
		this.atk.add(mod);
	}

	public void setAtk(Drawable source, int atk, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), atk, expiration);
		this.atk.remove(mod);
		this.atk.add(mod);
	}

	public int getDef() {
		return (int) sum(def);
	}

	public void setDef(Drawable source, int def) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), def);
		this.def.remove(mod);
		this.def.add(mod);
	}

	public void setDef(Drawable source, int def, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), def, expiration);
		this.def.remove(mod);
		this.def.add(mod);
	}

	public int getDodge() {
		return (int) sum(dodge);
	}

	public void setDodge(Drawable source, int dodge) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), dodge);
		this.dodge.remove(mod);
		this.dodge.add(mod);
	}

	public void setDodge(Drawable source, int dodge, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), dodge, expiration);
		this.dodge.remove(mod);
		this.dodge.add(mod);
	}

	public int getBlock() {
		return (int) sum(block);
	}

	public void setBlock(Drawable source, int block) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), block);
		this.block.remove(mod);
		this.block.add(mod);
	}

	public void setBlock(Drawable source, int block, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), block, expiration);
		this.block.remove(mod);
		this.block.add(mod);
	}

	public double getPower() {
		return sum(power);
	}

	public void setPower(Drawable source, int power) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), power);
		this.power.remove(mod);
		this.power.add(mod);
	}

	public void setPower(Drawable source, int power, int expiration) {
		AttrMod mod = new AttrMod(source, source.getSlot().getIndex(), power, expiration);
		this.power.remove(mod);
		this.power.add(mod);
	}

	public void setFlag(Flag flag, boolean value) {
		if (value) {
			flags.add(flag);
		} else {
			flags.remove(flag);
		}
	}

	public void setFlag(Flag flag, boolean value, boolean permanent) {
		if (value) {
			(permanent ? permFlags : flags).add(flag);
		} else {
			(permanent ? permFlags : flags).remove(flag);
		}
	}

	public boolean hasFlag(Flag flag) {
		return flags.contains(flag) || permFlags.contains(flag);
	}

	public boolean popFlag(Flag flag) {
		return flags.remove(flag) || permFlags.contains(flag);
	}

	public JSONObject getData() {
		return data;
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public Card getVanity() {
		return vanity;
	}

	public void setVanity(Card vanity) {
		this.vanity = vanity;
	}

	public String getWrite() {
		return write;
	}

	public void setWrite(String write) {
		this.write = write;
	}

	public String getDescription(I18N locale) {
		if (description == null || description.isBlank()) return description;

		return DAO.find(LocalizedDescription.class, new LocalizedDescId(description, locale)).getDescription();
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEffect() {
		return effect;
	}

	public void setEffect(String effect) {
		this.effect = effect;
	}

	@SuppressWarnings("unchecked")
	public void expireMods() {
		Predicate<AttrMod> check = mod -> {
			if (mod.expiration() != null) {
				return mod.expiration().decrementAndGet() <= 0;
			}

			return false;
		};

		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			try {
				if (f.get(this) instanceof HashSet s) {
					s.removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	private double sum(Set<AttrMod> mods) {
		double out = 0;
		for (AttrMod mod : mods) {
			out += mod.value();
		}

		return Calc.round(out, 1);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CardExtra cardExtra = (CardExtra) o;
		return Objects.equals(mana, cardExtra.mana) && Objects.equals(blood, cardExtra.blood) && Objects.equals(atk, cardExtra.atk) && Objects.equals(def, cardExtra.def) && Objects.equals(dodge, cardExtra.dodge) && Objects.equals(block, cardExtra.block) && Objects.equals(flags, cardExtra.flags) && Objects.equals(permFlags, cardExtra.permFlags) && Objects.equals(data, cardExtra.data) && race == cardExtra.race && Objects.equals(vanity, cardExtra.vanity) && Objects.equals(write, cardExtra.write) && Objects.equals(description, cardExtra.description) && Objects.equals(effect, cardExtra.effect);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mana, blood, atk, def, dodge, block, flags, permFlags, data, race, vanity, write, description, effect);
	}
}
