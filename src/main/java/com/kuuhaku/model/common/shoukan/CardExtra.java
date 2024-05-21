/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.common.ConditionalVar;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.id.LocalizedId;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.LocalizedDescription;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.lang3.BooleanUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CardExtra implements Cloneable {
	private final CumValue mana;
	private final CumValue blood;
	private final CumValue sacrifices;

	private final CumValue atk;
	private final CumValue dfs;
	private final CumValue dodge;
	private final CumValue block;

	private final CumValue lifesteal;
	private final CumValue thorns;

	private final CumValue costMult;
	private final CumValue attrMult;
	private final CumValue power;

	private final CumValue tier;

	private final Flags flags;

	private final JSONObject data;
	private final JSONObject perm;
	private final ListOrderedSet<String> curses;

	private Race race = null;
	private Card vanity = null;
	private Senshi disguise = null;

	private ConditionalVar<Callable<String>> write = new ConditionalVar<>();

	private Drawable<?> source = null;
	private String description = null;
	private String effect = null;

	private transient Field[] fieldCache = null;

	public CardExtra(
			CumValue mana, CumValue blood, CumValue sacrifices,
			CumValue atk, CumValue dfs, CumValue dodge,
			CumValue block, CumValue tier, CumValue lifesteal,
			CumValue thorns, CumValue costMult, CumValue attrMult,
			CumValue power, Flags flags, JSONObject data, JSONObject perm,
			ListOrderedSet<String> curses
	) {
		this.mana = mana;
		this.blood = blood;
		this.sacrifices = sacrifices;
		this.atk = atk;
		this.dfs = dfs;
		this.dodge = dodge;
		this.block = block;
		this.lifesteal = lifesteal;
		this.thorns = thorns;
		this.tier = tier;
		this.costMult = costMult;
		this.attrMult = attrMult;
		this.power = power;
		this.flags = flags;
		this.data = data;
		this.perm = perm;
		this.curses = curses;
	}

	public CardExtra() {
		this(
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.flat(),
				CumValue.mult(),
				CumValue.mult(),
				CumValue.mult(),
				new Flags(),
				new JSONObject(),
				new JSONObject(),
				ListOrderedSet.listOrderedSet(BondedList.withBind((s, it) -> !s.isBlank()))
		);
	}

	public CumValue getMana() {
		return mana;
	}

	public CumValue getBlood() {
		return blood;
	}

	public CumValue getSacrifices() {
		return sacrifices;
	}

	public CumValue getAtk() {
		return atk;
	}

	public CumValue getDfs() {
		return dfs;
	}

	public CumValue getDodge() {
		return dodge;
	}

	public CumValue getBlock() {
		return block;
	}

	public CumValue getLifesteal() {
		return lifesteal;
	}

	public CumValue getThorns() {
		return thorns;
	}

	public CumValue getCostMult() {
		return costMult;
	}

	public CumValue getAttrMult() {
		return attrMult;
	}

	public CumValue getPower() {
		return power;
	}

	public CumValue getTier() {
		return tier;
	}

	public Flags getFlags() {
		return flags;
	}

	public void setFlag(Flag flag, boolean value, boolean permanent) {
		if (value) flags.set(null, flag, permanent);
		else flags.unset(null, flag, permanent);
	}

	public void setFlag(Drawable<?> source, Flag flag, boolean value, boolean permanent) {
		if (value) flags.set(source, flag, permanent);
		else flags.unset(source, flag, permanent);
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
		if (write == null) return "";

		try {
			String out = write.getValue().call();
			if (out == null) {
				write = null;
				return "";
			}

			return out;
		} catch (Exception e) {
			return "";
		}
	}

	public void setWrite(Callable<String> write) {
		this.write.setValue(write);
	}

	public void setWrite(Callable<String> write, BooleanSupplier condition) {
		this.write.setValue(write);
		this.write.setCondition(condition);
	}

	public void setWrite(Callable<String> write, Object ref) {
		setWrite(write, () -> {
			if (ref == null) return false;

			return !switch (ref) {
				case Collection<?> c -> c.isEmpty();
				case String s -> s.isBlank();
				case Number n -> n.doubleValue() == 0;
				default -> true;
			};
		});
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
		return desc == null ? description : desc.toString();
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
		Predicate<ValueMod> check = mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		};

		removeIf(check);
	}

	public void clear() {
		removeIf(o -> !(o instanceof PermMod));
	}

	public void clear(Drawable<?> source) {
		removeIf(o -> !(o instanceof PermMod) && Objects.equals(o.getSource(), source));
	}

	public void removeIf(Predicate<ValueMod> check) {
		if (fieldCache == null) {
			fieldCache = getClass().getDeclaredFields();
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof CumValue cv) {
					cv.values().removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}

		flags.clearExpired();
	}

	private double sum(Set<ValueMod> mods) {
		double out = 0;
		for (ValueMod mod : mods) {
			out += mod.getValue();
		}

		return Calc.round(out, 2);
	}

	@Override
	public CardExtra clone() {
		CardExtra clone = new CardExtra(
				mana.clone(),
				blood.clone(),
				sacrifices.clone(),
				atk.clone(),
				dfs.clone(),
				dodge.clone(),
				block.clone(),
				lifesteal.clone(),
				thorns.clone(),
				tier.clone(),
				attrMult.clone(),
				costMult.clone(),
				power.clone(),
				flags.clone(),
				data.clone(),
				perm.clone(),
				ListOrderedSet.listOrderedSet(BondedList.withBind((s, it) -> !s.isBlank()))
		);

		clone.race = race;
		clone.source = source;
		clone.description = description;
		clone.effect = effect;

		return clone;
	}
}
