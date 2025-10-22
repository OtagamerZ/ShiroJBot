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

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.util.Calc;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CumValue implements Iterable<ValueMod> {
	private final Set<ValueMod> values = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public double multiplier() {
		return 1 + offset();
	}

	public double offset() {
		double inc = 0;
		double mult = 1;

		for (ValueMod mod : values) {
			if (mod.getSource() instanceof EffectHolder<?> eh) {
				if (!eh.hasEffect() || eh.getHand().getLockTime(Lock.EFFECT) > 0) {
					continue;
				}
			}

			switch (mod) {
				case IncMod _ -> inc += mod.getValue();
				case MultMod _ -> mult *= 1 + mod.getValue();
				default -> throw new IllegalStateException("Unexpected value: " + mod);
			}
		}

		return Calc.round(1 * (1 + inc) * mult - 1, 2);
	}

	public double get() {
		return apply(0);
	}

	public double apply(double base) {
		for (ValueMod mod : values) {
			if (mod.getSource() instanceof EffectHolder<?> eh) {
				if (!eh.hasEffect() || eh.getHand().getLockTime(Lock.EFFECT) > 0) {
					continue;
				}
			}

			if (mod instanceof FlatMod || mod instanceof DynamicMod) {
				base += mod.getValue();
			}
		}

		return Calc.round(base * multiplier(), 2);
	}

	@SuppressWarnings("unchecked")
	public <T extends ValueMod> T get(Drawable<?> source, Class<T> klass) {
		for (ValueMod mod : values) {
			if (mod.getClass() == klass && Objects.equals(source, mod.getSource())) {
				return (T) mod;
			}
		}

		if (klass == FlatMod.class) {
			return (T) new FlatMod(source, 0);
		} else if (klass == IncMod.class) {
			return (T) new IncMod(source, 0);
		} else if (klass == MultMod.class) {
			return (T) new MultMod(source, 0);
		} else if (klass == DynamicMod.class) {
			return (T) new DynamicMod(source, () -> 0);
		}

		throw new IllegalStateException("Unexpected value: " + klass);
	}

	@SuppressWarnings("unchecked")
	public <T extends ValueMod> T set(T value) {
		for (ValueMod mod : values) {
			if (mod.isPermanent() && mod.getClass() == value.getClass()) {
				mod.setValue(mod.getValue() + value.getValue());
				return (T) mod;
			}
		}

		values.remove(value);
		values.add(value);
		return value;
	}

	public <T extends ValueMod> ValueMod leftShift(T value) {
		return set(value);
	}

	public Set<ValueMod> values() {
		return values;
	}

	@NotNull
	@Override
	public Iterator<ValueMod> iterator() {
		return values.iterator();
	}

	public void copyTo(CumValue to) {
		for (ValueMod v : this) {
			to.values.add(v.copy());
		}
	}
}
