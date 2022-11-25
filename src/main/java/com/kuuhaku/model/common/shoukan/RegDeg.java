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
	private final BondedList<ValueOverTime> values = BondedList.withBind((v, it) -> {
		v.setValue(reduce(v.getClass(), v.getValue()));
		return true;
	});

	public List<ValueOverTime> getValues() {
		return values;
	}

	public void add(ValueOverTime vot) {
		values.add(vot);
	}

	public void leftShift(ValueOverTime vot) {
		add(vot);
	}

	public void leftShift(Number number) {
		int value = number.intValue();

		if (value < 0) {
			add(new Degen(-value, 0.1));
		} else if (value > 0) {
			add(new Regen(value, 0.1));
		}
	}

	public void leftShift(List<Number> number) {
		int value = number.get(0).intValue();
		double mult = number.get(1).doubleValue();

		if (value < 0) {
			add(new Degen(-value, mult));
		} else if (value > 0) {
			add(new Regen(value, mult));
		}
	}

	public <T extends ValueOverTime> int reduce(Class<T> klass, int val) {
		if (val == 0) return 0;

		for (ValueOverTime vot : values) {
			if (!vot.getClass().equals(klass)) {
				if ((val = vot.reduce(val)) <= 0) {
					break;
				}
			}
		}

		try {
			return val;
		} finally {
			values.removeIf(v -> v.getValue() <= 0);
		}
	}

	public int next() {
		try {
			return values.stream().mapToInt(ValueOverTime::next).sum();
		} finally {
			values.removeIf(v -> v.getValue() <= 0);
		}
	}

	public int peek() {
		return values.stream().mapToInt(ValueOverTime::peek).sum();
	}
}
