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

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.common.TimedMap;
import com.kuuhaku.model.common.shoukan.CumValue;
import com.kuuhaku.model.common.shoukan.ValueMod;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.records.dunhun.CombatContext;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ActorModifiers implements Iterable<CumValue> {
	private final CumValue maxHp = new CumValue();
	private final CumValue maxAp = new CumValue();
	private final CumValue initiative = new CumValue();
	private final CumValue critical = new CumValue();
	private final CumValue spellDamage = new CumValue();

	private final CumValue aggro = new CumValue();
	private final CumValue magicFind = new CumValue();

	private final CumValue healing = new CumValue();
	private final CumValue damageTaken = new CumValue();

	private final Set<EffectBase> permEffects = new HashSet<>();
	private final TimedMap<EffectBase> tempEffects = new TimedMap<>();
	private Actor<?> channeled;

	private final Field[] fieldCache = getClass().getDeclaredFields();

	public CumValue getMaxHp() {
		return maxHp;
	}

	public CumValue getMaxAp() {
		return maxAp;
	}

	public CumValue getInitiative() {
		return initiative;
	}

	public CumValue getCritical() {
		return critical;
	}

	public CumValue getSpellDamage() {
		return spellDamage;
	}

	public CumValue getAggro() {
		return aggro;
	}

	public CumValue getMagicFind() {
		return magicFind;
	}

	public CumValue getHealing() {
		return healing;
	}

	public CumValue getDamageTaken() {
		return damageTaken;
	}

	public void addEffect(Actor<?> source, ThrowingBiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		addEffect(source, effect, -1, -1, triggers);
	}

	public void addEffect(Actor<?> source, ThrowingBiConsumer<EffectBase, CombatContext> effect, int duration, int limit, Trigger... triggers) {
		if (triggers.length == 0 && duration < 0 && limit < 0) {
			permEffects.add(new PersistentEffect(source, effect));
			return;
		}

		tempEffects.add(new TriggeredEffect(source, limit, effect, triggers), duration);
	}

	public Set<EffectBase> getEffects() {
		return SetUtils.union(permEffects, tempEffects.getValues());
	}

	public Set<EffectBase> getPermEffects() {
		return permEffects;
	}

	public TimedMap<EffectBase> getTempEffects() {
		return tempEffects;
	}

	public Actor<?> getChanneled() {
		return channeled;
	}

	public void setChanneled(Actor<?> channeled) {
		this.channeled = channeled;
	}

	public void expireMods(Actor<?> act) {
		tempEffects.reduceTime();

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
		permEffects.removeIf(EffectBase::isClosed);
		tempEffects.getValues().removeIf(EffectBase::isClosed);

		for (Gear g : act.getEquipment()) {
			g.getModifiers().removeIf(check);
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof CumValue cv) {
					cv.values().removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	@Override
	public @NotNull Iterator<CumValue> iterator() {
		return Arrays.stream(fieldCache)
				.flatMap(f -> {
					try {
						if (f.get(this) instanceof CumValue cv) {
							return Stream.of(cv);
						}
					} catch (IllegalAccessException ignore) {
					}

					return null;
				})
				.filter(Objects::nonNull)
				.iterator();
	}

	public void copyFrom(ActorModifiers modifiers) {
		Iterator<CumValue> origMods = modifiers.iterator();
		for (CumValue mod : this) {
			origMods.next().copyTo(mod);
		}

		permEffects.addAll(modifiers.getPermEffects());
		for (Map.Entry<EffectBase, Integer> e : modifiers.getTempEffects()) {
			tempEffects.add(e.getKey(), e.getValue());
		}

		if (modifiers.getChanneled() != null) {
			this.channeled = modifiers.getChanneled();
		}
	}
}
