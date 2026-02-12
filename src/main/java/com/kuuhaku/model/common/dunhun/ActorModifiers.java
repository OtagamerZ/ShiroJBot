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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ActorModifiers {
	private final Actor<?> parent;
	private final Set<EffectProperties<?>> effects = new HashSet<>();
	private final ActorModifiers summon;

	public ActorModifiers(Actor<?> parent) {
		this(parent, false);
	}

	public ActorModifiers(Actor<?> parent, boolean minionStats) {
		this.parent = parent;
		this.summon = minionStats ? null : new ActorModifiers(parent, true);
	}

	private double accumulate(double base, Function<EffectProperties<?>, ValueMod> extractor) {
		Set<EffectProperties<?>> inherited = Set.of();
		if (parent.isMinion()) {
			inherited = parent.getMaster().getModifiers().summon.effects;
		}

		Iterator<EffectProperties<?>> it = IteratorUtils.chainedIterator(
				effects.iterator(),
				inherited.iterator()
		);

		double flat = 0, inc = 0, mult = 1;
		while (it.hasNext()) {
			ValueMod mod = extractor.apply(it.next());
			if (mod == null) continue;

			switch (mod) {
				case FlatMod m -> flat += m.getValue();
				case IncMod m -> inc += m.getValue();
				case MultMod m -> mult *= 1 + m.getValue();
				default -> {
				}
			}
		}

		return (base + flat) * (1 + inc) * mult;
	}

	public double getMaxHp() {
		return getMaxHp(0);
	}

	public double getMaxHp(double base) {
		return accumulate(base, EffectProperties::getMaxHp);
	}

	public double getMaxAp() {
		return getMaxAp(0);
	}

	public double getMaxAp(double base) {
		return accumulate(base, EffectProperties::getMaxAp);
	}

	public double getDamage() {
		return getDamage(0);
	}

	public double getDamage(double base) {
		return accumulate(base, EffectProperties::getDamage);
	}

	public double getDefense() {
		return getDefense(0);
	}

	public double getDefense(double base) {
		return accumulate(base, EffectProperties::getDefense);
	}

	public double getDodge() {
		return getDodge(0);
	}

	public double getDodge(double base) {
		return accumulate(base, EffectProperties::getDodge);
	}

	public double getParry() {
		return getParry(0);
	}

	public double getParry(double base) {
		return accumulate(base, EffectProperties::getParry);
	}

	public double getPower() {
		return getPower(0);
	}

	public double getPower(double base) {
		return accumulate(base, EffectProperties::getPower);
	}

	public double getInitiative() {
		return getInitiative(0);
	}

	public double getInitiative(double base) {
		return accumulate(base, EffectProperties::getInitiative);
	}

	public double getCritical() {
		return getCritical(0);
	}

	public double getCritical(double base) {
		return accumulate(base, EffectProperties::getCritical);
	}

	public double getSpellDamage() {
		return getSpellDamage(0);
	}

	public double getSpellDamage(double base) {
		return accumulate(base, EffectProperties::getSpellDamage);
	}

	public double getAggro() {
		return getAggro(0);
	}

	public double getAggro(double base) {
		return accumulate(base, EffectProperties::getAggro);
	}

	public double getMagicFind() {
		return getMagicFind(0);
	}

	public double getMagicFind(double base) {
		return accumulate(base, EffectProperties::getMagicFind);
	}

	public double getHealing() {
		return getHealing(0);
	}

	public double getHealing(double base) {
		return accumulate(base, EffectProperties::getHealing);
	}

	public double getDamageTaken() {
		return getDamageTaken(0);
	}

	public double getDamageTaken(double base) {
		return accumulate(base, EffectProperties::getDamageTaken);
	}

	public double getMaxSummons(double base) {
		return accumulate(base, EffectProperties::getMaxSummons);
	}

	public UniqueProperties<?> getEffect(Object id) {
		return (UniqueProperties<?>) effects.stream()
				.filter(e -> e instanceof UniqueProperties<?> u && Objects.equals(u.getIdentifier(), id))
				.findFirst().orElse(null);
	}

	public Collection<EffectProperties<?>> getEffects() {
		if (parent.isMinion()) {
			return CollectionUtils.union(effects, parent.getMaster().getModifiers().summon.effects);
		}

		return effects;
	}

	public ActorModifiers getSummon() {
		return summon;
	}

	public void leftShift(EffectProperties<?> effect) {
		addEffect(effect);
	}

	public void addEffect(EffectProperties<?> effect) {
		EffectProperties<?> curr = effects.stream()
				.filter(e -> Objects.equals(e, effect))
				.findFirst().orElse(null);

		if (curr != null) {
			if (curr.getPriority() > effect.getPriority()) return;

			effects.remove(effect);
			effects.add(effect);
		} else {
			effects.add(effect);
		}
	}

	public void expireMods() {
		for (EffectProperties<?> effect : effects) {
			effect.decExpiration();
		}

		removeIf(mod -> {
			mod.decExpiration();
			return mod.isExpired();
		});
	}

	public void clear() {
		effects.removeIf(e -> !(e instanceof PermanentProperties<?>));
		removeIf(_ -> true);
	}

	public void removeIf(Predicate<ValueMod> check) {
		for (Gear g : parent.getEquipment()) {
			g.getModifiers().removeIf(check);
		}

		effects.removeIf(EffectProperties::isSafeToRemove);
	}

	public void copyFrom(ActorModifiers modifiers) {
		effects.addAll(modifiers.effects);
	}
}
