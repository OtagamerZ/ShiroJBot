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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class CumValue implements Iterable<ValueMod>, Cloneable {
	final Set<ValueMod> values = new HashSet<>();
	final boolean flat;

	private CumValue(boolean flat) {
		this.flat = flat;
	}

	public static CumValue flat() {
		return new CumValue(true);
	}

	public static CumValue mult() {
		return new CumValue(false);
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
		if (klass == Short.class) {
			return klass.cast((short) get());
		} else if (klass == Integer.class) {
			return klass.cast((int) get());
		} else if (klass == Long.class) {
			return klass.cast((long) get());
		} else if (klass == Float.class) {
			return klass.cast((float) get());
		} else if (klass == Double.class) {
			return klass.cast(get());
		} else if (klass == Byte.class) {
			return klass.cast((byte) get());
		}

		throw new ClassCastException();
	}

	public ValueMod rightShift(Drawable<?> source) {
		return get(source);
	}

	public void set(double value) {
		for (ValueMod mod : this.values) {
			if (mod instanceof PermMod) {
				mod.setValue(mod.getValue() + value);
				return;
			}
		}

		this.values.add(new PermMod(value));
	}

	public void set(Drawable<?> source, double value) {
		ValueMod mod = new ValueMod(source, value);
		this.values.remove(mod);
		this.values.add(mod);
	}

	public void set(Drawable<?> source, double value, int expiration) {
		ValueMod mod = new ValueMod(source, value, expiration);
		this.values.remove(mod);
		this.values.add(mod);
	}

	public void leftShift(Number value) {
		set(value.doubleValue());
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

	@Override
	public CumValue clone() {
		CumValue clone = new CumValue(flat);
		for (ValueMod v : this) {
			clone.values.add(v.clone());
		}

		return clone;
	}
}
