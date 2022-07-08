/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.common.BondedList;

import java.util.List;

public class RegDeg {
	private final List<ValueOverTime> values = new BondedList<>(v -> v.setValue(reduce(v.getClass(), v.getValue())));

	public List<ValueOverTime> getValues() {
		return values;
	}

	public void add(ValueOverTime vot) {
		values.add(vot);
	}

	public <T extends ValueOverTime> int reduce(Class<T> klass, int val) {
		if (val == 0) return 0;

		for (ValueOverTime vot : values) {
			if (!vot.getClass().equals(klass)) {
				if ((val = vot.reduce(val)) == 0) {
					break;
				}
			}
		}

		try {
			return val;
		} finally {
			values.removeIf(v -> v.getValue() == 0);
		}
	}

	public int next() {
		try {
			return values.stream().mapToInt(ValueOverTime::next).sum();
		} finally {
			values.removeIf(v -> v.getValue() == 0);
		}
	}

	public int peek() {
		return values.stream().mapToInt(ValueOverTime::peek).sum();
	}
}
