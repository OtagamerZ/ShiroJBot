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
	private final HashSet<AttrMod> def;

	private final HashSet<AttrMod> dodge;
	private final HashSet<AttrMod> block;

	private final HashSet<AttrMod> attrMult;

	private final HashSet<AttrMod> tier;

	private final EnumSet<Flag> flags;
	private final EnumSet<Flag> permFlags;

	private final JSONObject data;
	private final JSONObject perm;
	private final ListOrderedSet<String> curses;

	private Race race = null;
	private Card vanity = null;

	private String write = null;

	private Drawable<?> source = null;
	private String description = null;
	private String effect = null;

	private transient Field[] fieldCache = null;

	public CardExtra(
			HashSet<AttrMod> mana, HashSet<AttrMod> blood, HashSet<AttrMod> sacrifices,
			HashSet<AttrMod> atk, HashSet<AttrMod> def, HashSet<AttrMod> dodge,
			HashSet<AttrMod> block, HashSet<AttrMod> attrMult, HashSet<AttrMod> tier,
			EnumSet<Flag> flags, EnumSet<Flag> permFlags, JSONObject data,
			JSONObject perm, ListOrderedSet<String> curses
	) {
		this.mana = mana;
		this.blood = blood;
		this.sacrifices = sacrifices;
		this.atk = atk;
		this.def = def;
		this.dodge = dodge;
		this.block = block;
		this.attrMult = attrMult;
		this.tier = tier;
		this.flags = flags;
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
				EnumSet.noneOf(Flag.class),
				EnumSet.noneOf(Flag.class),
				new JSONObject(),
				new JSONObject(),
				ListOrderedSet.listOrderedSet(BondedList.withCheck(s -> s != null && s.isBlank()))
		);
	}

	public int getMana() {
		return (int) sum(mana);
	}

	public void setMana(int mana) {
		for (AttrMod mod : this.mana) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + mana);
				return;
			}
		}

		AttrMod mod = new PermMod(mana);
		this.mana.remove(mod);
		this.mana.add(mod);
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
		return (int) sum(blood);
	}

	public void setBlood(int blood) {
		for (AttrMod mod : this.blood) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + blood);
				return;
			}
		}

		AttrMod mod = new PermMod(blood);
		this.blood.remove(mod);
		this.blood.add(mod);
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
		return (int) sum(sacrifices);
	}

	public void setSacrifices(int sacrifices) {
		for (AttrMod mod : this.sacrifices) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + sacrifices);
				return;
			}
		}

		AttrMod mod = new PermMod(sacrifices);
		this.sacrifices.remove(mod);
		this.sacrifices.add(mod);
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
		return (int) sum(atk);
	}

	public void setAtk(int atk) {
		for (AttrMod mod : this.atk) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + atk);
				return;
			}
		}

		AttrMod mod = new PermMod(atk);
		this.atk.remove(mod);
		this.atk.add(mod);
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

	public int getDef() {
		return (int) sum(def);
	}

	public void setDef(int def) {
		for (AttrMod mod : this.def) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + def);
				return;
			}
		}

		AttrMod mod = new PermMod(def);
		this.def.remove(mod);
		this.def.add(mod);
	}

	public void setDef(Drawable<?> source, int def) {
		AttrMod mod = new AttrMod(source, def);
		this.def.remove(mod);
		this.def.add(mod);
	}

	public void setDef(Drawable<?> source, int def, int expiration) {
		AttrMod mod = new AttrMod(source, def, expiration);
		this.def.remove(mod);
		this.def.add(mod);
	}

	public int getDodge() {
		return (int) sum(dodge);
	}

	public void setDodge(int dodge) {
		for (AttrMod mod : this.dodge) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + dodge);
				return;
			}
		}

		AttrMod mod = new PermMod(dodge);
		this.dodge.remove(mod);
		this.dodge.add(mod);
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
		return (int) sum(block);
	}

	public void setBlock(int block) {
		for (AttrMod mod : this.block) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + block);
				return;
			}
		}

		AttrMod mod = new PermMod(block);
		this.block.remove(mod);
		this.block.add(mod);
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
		return 1 + sum(attrMult);
	}

	public void setAttrMult(double attrMult) {
		for (AttrMod mod : this.attrMult) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + attrMult);
				return;
			}
		}

		AttrMod mod = new PermMod(attrMult);
		this.attrMult.remove(mod);
		this.attrMult.add(mod);
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

	public int getTier() {
		return (int) sum(tier);
	}

	public void setTier(int tier) {
		for (AttrMod mod : this.tier) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + tier);
				return;
			}
		}

		AttrMod mod = new PermMod(tier);
		this.tier.remove(mod);
		this.tier.add(mod);
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
		if (description == null || description.isBlank()) return description;

		return DAO.find(LocalizedDescription.class, new LocalizedId(description, locale)).getDescription();
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

	@SuppressWarnings("unchecked")
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

	private double sum(Set<AttrMod> mods) {
		double out = 0;
		for (AttrMod mod : mods) {
			out += mod.getValue();
		}

		return Calc.round(out, 1);
	}

	@Override
	public CardExtra clone() {
		@SuppressWarnings("rawtypes")
		Copier<HashSet, AttrMod> copier = new Copier<>(HashSet.class, AttrMod.class);

		CardExtra clone = new CardExtra(
				copier.makeCopy(mana),
				copier.makeCopy(blood),
				copier.makeCopy(sacrifices),
				copier.makeCopy(atk),
				copier.makeCopy(def),
				copier.makeCopy(dodge),
				copier.makeCopy(block),
				copier.makeCopy(attrMult),
				copier.makeCopy(tier),
				EnumSet.copyOf(flags),
				EnumSet.copyOf(permFlags),
				data.clone(),
				perm.clone(),
				ListOrderedSet.listOrderedSet(BondedList.withCheck(s -> s != null && s.isBlank()))
		);

		clone.race = race;
		clone.vanity = vanity;
		clone.write = write;
		clone.source = source;
		clone.description = description;
		clone.effect = effect;

		return clone;
	}
}
