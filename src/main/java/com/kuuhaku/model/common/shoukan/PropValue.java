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

package com.kuuhaku.model.common.shoukan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PropValue extends Number {
	private final List<Number> values;

	public PropValue(List<Number> values) {
		this.values = values;
	}

	public static PropValue from(Number n) {
		return new PropValue(new ArrayList<>()).add(n);
	}

	public PropValue add(Number n) {
		values.add(n);
		return this;
	}

	public Number getAt(int index) {
		return values.get(index);
	}

	@Override
	public int intValue() {
		return values.getFirst().intValue();
	}

	@Override
	public long longValue() {
		return values.getFirst().longValue();
	}

	@Override
	public float floatValue() {
		return values.getFirst().floatValue();
	}

	@Override
	public double doubleValue() {
		return values.getFirst().doubleValue();
	}

	public double negative() {
		return -doubleValue();
	}

	public <T extends Number> T asType(Class<T> klass) {
		if (values.isEmpty()) return klass.cast(0);

		if (klass == Short.class) {
			return klass.cast(shortValue());
		} else if (klass == Integer.class) {
			return klass.cast(intValue());
		} else if (klass == Long.class) {
			return klass.cast(longValue());
		} else if (klass == Float.class) {
			return klass.cast(floatValue());
		} else if (klass == Double.class) {
			return klass.cast(doubleValue());
		} else if (klass == Byte.class) {
			return klass.cast(byteValue());
		}

		throw new ClassCastException();
	}

	public List<Number> values() {
		return values;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (PropValue) obj;
		return Objects.equals(this.values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values);
	}
}
