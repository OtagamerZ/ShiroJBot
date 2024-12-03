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
import com.kuuhaku.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CumValue implements Iterable<ValueMod> {
	private final Set<ValueMod> values = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final boolean flat;

	private CumValue(boolean flat) {
		this.flat = flat;
	}

	public static CumValue flat() {
		return new CumValue(true);
	}

	public static CumValue mult() {
		return new CumValue(false);
	}

	public double raw() {
		return values.parallelStream()
				.mapToDouble(ValueMod::getValue)
				.sum();
	}

	public double get() {
		return accumulate(values);
	}

	public ValueMod get(Drawable<?> source) {
		for (ValueMod mod : values) {
			if (Objects.equals(source, mod.getSource())) {
				return mod;
			}
		}

		return new ValueMod(source, 0);
	}

	public <T extends Number> T asType(Class<T> klass) {
		return Utils.fromNumber(klass, get());
	}

	public ValueMod rightShift(Drawable<?> source) {
		return get(source);
	}

	public ValueMod set(double value) {
		for (ValueMod mod : this.values) {
			if (mod.isPermanent() && mod.getClass() == ValueMod.class) {
				mod.setValue(mod.getValue() + value);
				return mod;
			}
		}

		ValueMod mod = new ValueMod(value);
		this.values.add(mod);
		return mod;
	}

	public ValueMod set(Drawable<?> source, double value) {
		return set(source, value, -1);
	}

	public ValueMod set(Drawable<?> source, double value, int expiration) {
		ValueMod mod = new ValueMod(source, value, expiration);
		this.values.remove(mod);
		this.values.add(mod);
		return mod;
	}

	public ValueMod leftShift(Number value) {
		return set(value.doubleValue());
	}

	public ValueMod set(Supplier<Number> supplier) {
		for (ValueMod mod : this.values) {
			if (mod.isPermanent() && mod.getClass() == DynamicMod.class) {
				((DynamicMod) mod).addSupplier(supplier);
				return mod;
			}
		}

		DynamicMod mod = new DynamicMod(supplier);
		this.values.add(mod);
		return mod;
	}

	public ValueMod set(Drawable<?> source, Supplier<Number> supplier) {
		return set(source, supplier, -1);
	}

	public ValueMod set(Drawable<?> source, Supplier<Number> supplier, int expiration) {
		DynamicMod mod = new DynamicMod(source, supplier, expiration);
		this.values.remove(mod);
		this.values.add(mod);
		return mod;
	}

	public ValueMod leftShift(Supplier<Number> supplier) {
		return set(supplier);
	}

	public Set<ValueMod> values() {
		return values;
	}

	private double accumulate(Set<ValueMod> mods) {
		double out = flat ? 0 : 1;
		for (ValueMod mod : mods) {
			if (mod.getSource() instanceof EffectHolder<?> eh) {
				if (!eh.hasEffect() || eh.getHand().getLockTime(Lock.EFFECT) > 0) {
					continue;
				}
			}

			if (flat) out += mod.getValue();
			else out *= 1 + mod.getValue();
		}

		return Calc.round(out, 2);
	}

	@NotNull
	@Override
	public Iterator<ValueMod> iterator() {
		return values.iterator();
	}

	public void copyTo(CumValue to) {
		for (ValueMod v : this) {
			to.values.add(v.clone());
		}
	}
}
