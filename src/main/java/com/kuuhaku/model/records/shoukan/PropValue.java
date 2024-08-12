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

package com.kuuhaku.model.records.shoukan;

import java.util.ArrayList;
import java.util.List;

public record PropValue(List<Number> values) {
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

	public Number plus(Number other) {
		return values.getFirst().doubleValue() + other.doubleValue();
	}

	public Number minus(Number other) {
		return values.getFirst().doubleValue() - other.doubleValue();
	}

	public Number multiply(Number other) {
		return values.getFirst().doubleValue() * other.doubleValue();
	}

	public Number negative() {
		return -values.getFirst().doubleValue();
	}

	public Number div(Number other) {
		return values.getFirst().doubleValue() / other.doubleValue();
	}

	public <T extends Number> T asType(Class<T> klass) {
		if (values.isEmpty()) return klass.cast(0);

		if (klass == Short.class) {
			return klass.cast(values.getFirst().shortValue());
		} else if (klass == Integer.class) {
			return klass.cast(values.getFirst().intValue());
		} else if (klass == Long.class) {
			return klass.cast(values.getFirst().longValue());
		} else if (klass == Float.class) {
			return klass.cast(values.getFirst().floatValue());
		} else if (klass == Double.class) {
			return klass.cast(values.getFirst().doubleValue());
		} else if (klass == Byte.class) {
			return klass.cast(values.getFirst().byteValue());
		}

		throw new ClassCastException();
	}
}
