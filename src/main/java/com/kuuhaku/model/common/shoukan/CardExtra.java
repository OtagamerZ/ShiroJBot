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
import com.kuuhaku.model.persistent.localized.LocalizedDescription;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.id.LocalizedId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CardExtra implements Iterable<CumValue> {
	private final CumValue mana = new CumValue();
	private final CumValue blood = new CumValue();
	private final CumValue sacrifices = new CumValue();
	private final CumValue cost = new CumValue();

	private final CumValue atk = new CumValue();
	private final CumValue dfs = new CumValue();
	private final CumValue attr = new CumValue();

	private final CumValue dodge = new CumValue();
	private final CumValue parry = new CumValue();
	private final CumValue power = new CumValue();

	private final CumValue piercing = new CumValue();
	private final CumValue lifesteal = new CumValue();
	private final CumValue thorns = new CumValue();

	private final CumValue attacks = new CumValue();
	private final CumValue tier = new CumValue();

	private final Flags flags = new Flags();

	private final JSONObject data = new JSONObject();
	private final ListOrderedSet<String> curses = ListOrderedSet.listOrderedSet(BondedList.withBind((s, _) -> !s.isBlank()));

	private Race race = null;
	private Senshi disguise = null;

	private final ConditionalVar<Card> vanity = new ConditionalVar<>();
	private final ConditionalVar<Supplier<String>> write = new ConditionalVar<>();

	private Drawable<?> source = null;
	private String description = null;
	private String effect = null;

	private final Field[] fieldCache = getClass().getDeclaredFields();

	public CumValue getMana() {
		return mana;
	}

	public CumValue getBlood() {
		return blood;
	}

	public CumValue getSacrifices() {
		return sacrifices;
	}

	public CumValue getCost() {
		return cost;
	}

	public CumValue getAtk() {
		return atk;
	}

	public CumValue getDfs() {
		return dfs;
	}

	public CumValue getAttr() {
		return attr;
	}

	public CumValue getDodge() {
		return dodge;
	}

	public CumValue getParry() {
		return parry;
	}

	public CumValue getPower() {
		return power;
	}

	public CumValue getPiercing() {
		return piercing;
	}

	public CumValue getLifesteal() {
		return lifesteal;
	}

	public CumValue getThorns() {
		return thorns;
	}

	public CumValue getAttacks() {
		return attacks;
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
		return vanity.getValue();
	}

	public void setVanity(BooleanSupplier condition, Card vanity) {
		this.vanity.setValue(vanity);
		this.vanity.setCondition(condition);
	}

	public Senshi getDisguise() {
		return disguise;
	}

	public void setDisguise(Senshi disguise) {
		this.disguise = disguise;
	}

	public String getWrite() {
		try {
			Supplier<String> val = write.getValue();
			if (val == null) return "";

			return Utils.getOr((Object) val.get(), "").toString();
		} catch (Exception e) {
			return "";
		}
	}

	public void setWrite(Supplier<String> write) {
		this.write.setValue(write);
	}

	public void setWrite(Supplier<Object> condition, Supplier<String> write) {
		this.write.setValue(write);
		this.write.setCondition(() -> {
			Object ref = condition.get();
			if (ref == null) return false;

			return switch (ref) {
				case Collection<?> c -> !c.isEmpty();
				case String s -> !s.isBlank();
				case Number n -> n.doubleValue() != 0;
				case Boolean b -> b;
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
		removeIf(mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		});
	}

	public void clear() {
		removeIf(o -> !o.isPermanent());
	}

	public void clear(Drawable<?> source) {
		removeIf(o -> !o.isPermanent() && Objects.equals(o.getSource(), source));
	}

	public void removeIf(Predicate<ValueMod> check) {
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

	public CardExtra copy() {
		CardExtra clone = new CardExtra();

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof CumValue from && f.get(clone) instanceof CumValue to) {
					from.copyTo(to);
				}
			} catch (IllegalAccessException ignore) {
			}
		}

		flags.copyTo(clone.flags);
		clone.data.putAll(data.clone());

		clone.race = race;
		clone.source = source;
		clone.description = description;
		clone.effect = effect;

		return clone;
	}

	@Override
	public @NotNull Iterator<CumValue> iterator() {
		return Arrays.stream(fieldCache)
				.map(f -> {
					try {
						if (f.get(this) instanceof CumValue cv) {
							return cv;
						}
					} catch (IllegalAccessException ignore) {
					}

					return null;
				})
				.filter(Objects::nonNull)
				.iterator();
	}
}
