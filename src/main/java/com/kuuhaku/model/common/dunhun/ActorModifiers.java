/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.shoukan.FlatMod;
import com.kuuhaku.model.common.shoukan.IncMod;
import com.kuuhaku.model.common.shoukan.MultMod;
import com.kuuhaku.model.common.shoukan.ValueMod;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.records.dunhun.CachedValue;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ActorModifiers {
	private final Set<EffectProperties<?>> effects = new HashSet<>();

	private final Map<String, CachedValue> cache = new HashMap<>();
	private int cacheHash = 0;

	private double fetch(String field, double base, Function<EffectProperties<?>, ValueMod> extractor) {
		if (effects.hashCode() != cacheHash) {
			cache.clear();
			cacheHash = effects.hashCode();
		}

		return cache.computeIfAbsent(field, _ -> {
			double flat = 0, inc = 0, mult = 1;
			Iterator<ValueMod> it = effects.stream()
					.map(extractor)
					.filter(Objects::nonNull)
					.iterator();

			while (it.hasNext()) {
				switch (it.next()) {
					case FlatMod m -> flat += m.getValue();
					case IncMod m -> inc += m.getValue();
					case MultMod m -> mult *= m.getValue();
					default -> {
					}
				}
			}

			return new CachedValue(flat, inc, mult);
		}).apply(base);
	}

	public double getMaxHp() {
		return getMaxHp(0);
	}

	public double getMaxHp(double base) {
		return fetch("maxhp", base, EffectProperties::getMaxHp);
	}

	public double getMaxAp() {
		return getMaxAp(0);
	}

	public double getMaxAp(double base) {
		return fetch("maxap", base, EffectProperties::getMaxAp);
	}

	public double getDamage() {
		return getDamage(0);
	}

	public double getDamage(double base) {
		return fetch("damage", base, EffectProperties::getDamage);
	}

	public double getDefense() {
		return getDefense(0);
	}

	public double getDefense(double base) {
		return fetch("defense", base, EffectProperties::getDefense);
	}

	public double getDodge() {
		return getDodge(0);
	}

	public double getDodge(double base) {
		return fetch("dodge", base, EffectProperties::getDodge);
	}

	public double getParry() {
		return getParry(0);
	}

	public double getParry(double base) {
		return fetch("parry", base, EffectProperties::getParry);
	}

	public double getPower() {
		return getPower(0);
	}

	public double getPower(double base) {
		return fetch("power", base, EffectProperties::getPower);
	}

	public double getInitiative() {
		return getInitiative(0);
	}

	public double getInitiative(double base) {
		return fetch("initiative", base, EffectProperties::getInitiative);
	}

	public double getCritical() {
		return getCritical(0);
	}

	public double getCritical(double base) {
		return fetch("critical", base, EffectProperties::getCritical);
	}

	public double getSpellDamage() {
		return getSpellDamage(0);
	}

	public double getSpellDamage(double base) {
		return fetch("spelldamage", base, EffectProperties::getSpellDamage);
	}

	public double getAggro() {
		return getAggro(0);
	}

	public double getAggro(double base) {
		return fetch("aggro", base, EffectProperties::getAggro);
	}

	public double getMagicFind() {
		return getMagicFind(0);
	}

	public double getMagicFind(double base) {
		return fetch("magicfind", base, EffectProperties::getMagicFind);
	}

	public double getHealing() {
		return getHealing(0);
	}

	public double getHealing(double base) {
		return fetch("healing", base, EffectProperties::getHealing);
	}

	public double getDamageTaken() {
		return getDamageTaken(0);
	}

	public double getDamageTaken(double base) {
		return fetch("damagetaken", base, EffectProperties::getDamageTaken);
	}

	public Set<EffectProperties<?>> getEffects() {
		return effects;
	}

	public void addEffect(EffectProperties<?> effect) {
		EffectProperties<?> curr = effects.stream()
				.filter(e -> Objects.equals(e, effect))
				.findFirst().orElse(null);

		if (curr != null) {
			if (curr.getPriority() > effect.getPriority()) return;

			effects.remove(effect);
			effects.add(effect);
		}
	}

	public void expireMods(Actor<?> act) {
		removeIf(act, mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		});
	}

	public void clear(Actor<?> act) {
		removeIf(act, _ -> true);
	}

	public void removeIf(Actor<?> act, Predicate<ValueMod> check) {
		act.getSenshi().getStats().removeIf(check);

		for (Gear g : act.getEquipment()) {
			g.getModifiers().removeIf(check);
		}

		effects.removeIf(EffectProperties::isSafeToRemove);
	}

	public void copyFrom(ActorModifiers modifiers) {
		effects.addAll(modifiers.getEffects());
	}
}
