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
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.id.LocalizedId;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.LocalizedDescription;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Copier;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import org.apache.commons.collections4.set.ListOrderedSet;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class CardExtra implements Cloneable {
	private final HashSet<AttrMod> mana;
	private final HashSet<AttrMod> blood;
	private final HashSet<AttrMod> sacrifices;

	private final HashSet<AttrMod> atk;
	private final HashSet<AttrMod> dfs;

	private final HashSet<AttrMod> dodge;
	private final HashSet<AttrMod> block;

	private final HashSet<AttrMod> attrMult;
	private final HashSet<AttrMod> power;

	private final HashSet<AttrMod> tier;

	private final EnumSet<Flag> flags;
	private final EnumSet<Flag> tempFlags;
	private final EnumSet<Flag> permFlags;

	private final JSONObject data;
	private final JSONObject perm;
	private final ListOrderedSet<String> curses;

	private Race race = null;
	private Card vanity = null;
	private Senshi disguise = null;

	private String write = null;

	private Drawable<?> source = null;
	private String description = null;
	private String effect = null;

	private transient Field[] fieldCache = null;

	public CardExtra(
			HashSet<AttrMod> mana, HashSet<AttrMod> blood, HashSet<AttrMod> sacrifices,
			HashSet<AttrMod> atk, HashSet<AttrMod> dfs, HashSet<AttrMod> dodge,
			HashSet<AttrMod> block, HashSet<AttrMod> attrMult, HashSet<AttrMod> tier,
			HashSet<AttrMod> power, EnumSet<Flag> flags, EnumSet<Flag> tempFlags,
			EnumSet<Flag> permFlags, JSONObject data, JSONObject perm,
			ListOrderedSet<String> curses
	) {
		this.mana = mana;
		this.blood = blood;
		this.sacrifices = sacrifices;
		this.atk = atk;
		this.dfs = dfs;
		this.dodge = dodge;
		this.block = block;
		this.attrMult = attrMult;
		this.power = power;
		this.tier = tier;
		this.flags = flags;
		this.tempFlags = tempFlags;
		this.permFlags = permFlags;
		this.data = data;
		this.perm = perm;
		this.curses = curses;
	}

	public CardExtra() {
		this(
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				new HashSet<>(),
				EnumSet.noneOf(Flag.class),
				EnumSet.noneOf(Flag.class),
				EnumSet.noneOf(Flag.class),
				new JSONObject(),
				new JSONObject(),
				ListOrderedSet.listOrderedSet(BondedList.withBind((s, it) -> !s.isBlank()))
		);
	}

	public int getMana() {
		return Calc.round(sum(mana));
	}

	public AttrMod getMana(Drawable<?> source) {
		for (AttrMod mod : mana) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setMana(int mana) {
		for (AttrMod mod : this.mana) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + mana);
				return;
			}
		}

		this.mana.add(new PermMod(mana));
	}

	public void setMana(Drawable<?> source, int mana) {
		AttrMod mod = new AttrMod(source, mana);
		this.mana.remove(mod);
		this.mana.add(mod);
	}

	public void setMana(Drawable<?> source, int mana, int expiration) {
		AttrMod mod = new AttrMod(source, mana, expiration);
		this.mana.remove(mod);
		this.mana.add(mod);
	}

	public int getBlood() {
		return Calc.round(sum(blood));
	}

	public AttrMod getBlood(Drawable<?> source) {
		for (AttrMod mod : blood) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setBlood(int blood) {
		for (AttrMod mod : this.blood) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + blood);
				return;
			}
		}

		this.blood.add(new PermMod(blood));
	}

	public void setBlood(Drawable<?> source, int blood) {
		AttrMod mod = new AttrMod(source, blood);
		this.blood.remove(mod);
		this.blood.add(mod);
	}

	public void setBlood(Drawable<?> source, int blood, int expiration) {
		AttrMod mod = new AttrMod(source, blood, expiration);
		this.blood.remove(mod);
		this.blood.add(mod);
	}

	public int getSacrifices() {
		return Calc.round(sum(sacrifices));
	}

	public AttrMod getSacrifices(Drawable<?> source) {
		for (AttrMod mod : sacrifices) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setSacrifices(int sacrifices) {
		for (AttrMod mod : this.sacrifices) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + sacrifices);
				return;
			}
		}

		this.sacrifices.add(new PermMod(sacrifices));
	}

	public void setSacrifices(Drawable<?> source, int sacrifices) {
		AttrMod mod = new AttrMod(source, sacrifices);
		this.sacrifices.remove(mod);
		this.sacrifices.add(mod);
	}

	public void setSacrifices(Drawable<?> source, int sacrifices, int expiration) {
		AttrMod mod = new AttrMod(source, sacrifices, expiration);
		this.sacrifices.remove(mod);
		this.sacrifices.add(mod);
	}

	public int getAtk() {
		return Calc.round(sum(atk));
	}

	public AttrMod getAtk(Drawable<?> source) {
		for (AttrMod mod : atk) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setAtk(int atk) {
		for (AttrMod mod : this.atk) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + atk);
				return;
			}
		}

		this.atk.add(new PermMod(atk));
	}

	public void setAtk(Drawable<?> source, int atk) {
		AttrMod mod = new AttrMod(source, atk);
		this.atk.remove(mod);
		this.atk.add(mod);
	}

	public void setAtk(Drawable<?> source, int atk, int expiration) {
		AttrMod mod = new AttrMod(source, atk, expiration);
		this.atk.remove(mod);
		this.atk.add(mod);
	}

	public int getDfs() {
		return Calc.round(sum(dfs));
	}

	public AttrMod getDfs(Drawable<?> source) {
		for (AttrMod mod : dfs) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setDfs(int dfs) {
		for (AttrMod mod : this.dfs) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + dfs);
				return;
			}
		}

		this.dfs.add(new PermMod(dfs));
	}

	public void setDfs(Drawable<?> source, int dfs) {
		AttrMod mod = new AttrMod(source, dfs);
		this.dfs.remove(mod);
		this.dfs.add(mod);
	}

	public void setDfs(Drawable<?> source, int dfs, int expiration) {
		AttrMod mod = new AttrMod(source, dfs, expiration);
		this.dfs.remove(mod);
		this.dfs.add(mod);
	}

	public int getDodge() {
		return Calc.round(sum(dodge));
	}

	public AttrMod getDodge(Drawable<?> source) {
		for (AttrMod mod : dodge) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setDodge(int dodge) {
		for (AttrMod mod : this.dodge) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + dodge);
				return;
			}
		}

		this.dodge.add(new PermMod(dodge));
	}

	public void setDodge(Drawable<?> source, int dodge) {
		AttrMod mod = new AttrMod(source, dodge);
		this.dodge.remove(mod);
		this.dodge.add(mod);
	}

	public void setDodge(Drawable<?> source, int dodge, int expiration) {
		AttrMod mod = new AttrMod(source, dodge, expiration);
		this.dodge.remove(mod);
		this.dodge.add(mod);
	}

	public int getBlock() {
		return Calc.round(sum(block));
	}

	public AttrMod getBlock(Drawable<?> source) {
		for (AttrMod mod : block) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setBlock(int block) {
		for (AttrMod mod : this.block) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + block);
				return;
			}
		}

		this.block.add(new PermMod(block));
	}

	public void setBlock(Drawable<?> source, int block) {
		AttrMod mod = new AttrMod(source, block);
		this.block.remove(mod);
		this.block.add(mod);
	}

	public void setBlock(Drawable<?> source, int block, int expiration) {
		AttrMod mod = new AttrMod(source, block, expiration);
		this.block.remove(mod);
		this.block.add(mod);
	}

	public double getAttrMult() {
		System.out.println(attrMult);

		return 1 + sum(attrMult);
	}

	public AttrMod getAttrMult(Drawable<?> source) {
		for (AttrMod mod : attrMult) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setAttrMult(double attrMult) {
		for (AttrMod mod : this.attrMult) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + attrMult);
				return;
			}
		}

		this.attrMult.add(new PermMod(attrMult));
	}

	public void setAttrMult(Drawable<?> source, double attrMult) {
		AttrMod mod = new AttrMod(source, attrMult);
		this.attrMult.remove(mod);
		this.attrMult.add(mod);
	}

	public void setAttrMult(Drawable<?> source, double attrMult, int expiration) {
		AttrMod mod = new AttrMod(source, attrMult, expiration);
		this.attrMult.remove(mod);
		this.attrMult.add(mod);
	}

	public double getPower() {
		return 1 + sum(power) * (hasFlag(Flag.EMPOWERED) ? 1.5 : 1);
	}

	public AttrMod getPower(Drawable<?> source) {
		for (AttrMod mod : power) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setPower(double power) {
		for (AttrMod mod : this.power) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + power);
				return;
			}
		}

		this.power.add(new PermMod(power));
	}

	public void setPower(Drawable<?> source, double power) {
		AttrMod mod = new AttrMod(source, power);
		this.power.remove(mod);
		this.power.add(mod);
	}

	public void setPower(Drawable<?> source, double power, int expiration) {
		AttrMod mod = new AttrMod(source, power, expiration);
		this.power.remove(mod);
		this.power.add(mod);
	}

	public int getTier() {
		return Calc.round(sum(tier));
	}

	public AttrMod getTier(Drawable<?> source) {
		for (AttrMod mod : tier) {
			if (mod.getSource().equals(source)) {
				return mod;
			}
		}

		return new AttrMod(source, 0);
	}

	public void setTier(int tier) {
		for (AttrMod mod : this.tier) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + tier);
				return;
			}
		}

		this.tier.add(new PermMod(tier));
	}

	public void setTier(Drawable<?> source, int tier) {
		AttrMod mod = new AttrMod(source, tier);
		this.tier.remove(mod);
		this.tier.add(mod);
	}

	public void setTier(Drawable<?> source, int tier, int expiration) {
		AttrMod mod = new AttrMod(source, tier, expiration);
		this.tier.remove(mod);
		this.tier.add(mod);
	}

	public void setTFlag(Flag flag, boolean value) {
		if (hasFlag(flag) == value) return;

		tempFlags.add(flag);
	}

	public void clearTFlags() {
		tempFlags.clear();
	}

	public void setFlag(Flag flag, boolean value) {
		setFlag(flag, value, false);
	}

	public void setFlag(Flag flag, boolean value, boolean permanent) {
		if (hasFlag(flag) == value) return;

		if (value) {
			(permanent ? permFlags : flags).add(flag);
		} else {
			(permanent ? permFlags : flags).remove(flag);
		}
	}

	public boolean hasFlag(Flag flag) {
		return tempFlags.contains(flag) || flags.contains(flag) || permFlags.contains(flag);
	}

	public boolean popFlag(Flag flag) {
		return tempFlags.remove(flag) || flags.remove(flag) || permFlags.contains(flag);
	}

	public JSONObject getData() {
		return data;
	}

	public JSONObject getPerm() {
		return perm;
	}

	public ListOrderedSet<String> getCurses() {
		return curses;
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

	public Senshi getDisguise() {
		return disguise;
	}

	public void setDisguise(Senshi disguise) {
		this.disguise = disguise;
	}

	public String getWrite() {
		return Utils.getOr(write, "");
	}

	public void setWrite(String write) {
		this.write = write;
	}

	public Drawable<?> getSource() {
		return source;
	}

	public void setSource(Drawable<?> source) {
		this.source = source;
	}

	public String getDescription(I18N locale) {
		if (description == null || description.isBlank() || description.contains(" ")) return description;

		LocalizedDescription desc = DAO.find(LocalizedDescription.class, new LocalizedId(description, locale));
		return desc == null ? description : desc.getDescription();
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

	public void expireMods() {
		Predicate<AttrMod> check = mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		};

		removeExpired(check);
	}

	@SuppressWarnings({"unchecked"})
	public void removeExpired(Predicate<AttrMod> check) {
		if (fieldCache == null) {
			fieldCache = this.getClass().getDeclaredFields();
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof HashSet s) {
					s.removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void removeTemporary() {
		if (fieldCache == null) {
			fieldCache = this.getClass().getDeclaredFields();
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof HashSet s) {
					s.removeIf(o -> !(o instanceof PermMod));
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	private double sum(Set<AttrMod> mods) {
		double out = 0;
		for (AttrMod mod : mods) {
			out += mod.getValue();
		}

		return Calc.round(out, 2);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public CardExtra clone() {
		Copier<HashSet, AttrMod> copier = new Copier<>(HashSet.class, AttrMod.class);

		CardExtra clone = new CardExtra(
				copier.makeCopy(mana),
				copier.makeCopy(blood),
				copier.makeCopy(sacrifices),
				copier.makeCopy(atk),
				copier.makeCopy(dfs),
				copier.makeCopy(dodge),
				copier.makeCopy(block),
				copier.makeCopy(attrMult),
				copier.makeCopy(power),
				copier.makeCopy(tier),
				EnumSet.copyOf(flags),
				EnumSet.copyOf(tempFlags),
				EnumSet.copyOf(permFlags),
				data.clone(),
				perm.clone(),
				ListOrderedSet.listOrderedSet(BondedList.withBind((s, it) -> !s.isBlank()))
		);

		clone.removeTemporary();

		clone.race = race;
		clone.vanity = vanity;
		clone.disguise = disguise;
		clone.write = write;
		clone.source = source;
		clone.description = description;
		clone.effect = effect;

		return clone;
	}
}