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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.util.Calc;
import kotlin.Pair;

import java.util.concurrent.atomic.AtomicInteger;

public record RegDeg(Pair<Integer, AtomicInteger> value, double dpt, boolean pos) {
	public RegDeg(int value, double dpt) {
		this(new Pair<>(value, new AtomicInteger(value)), Calc.clamp(dpt, 0, 1), value > 0);
	}

	public int remaining() {
		return value.getSecond().get();
	}

	public int reduce(int val) {
		int half = val / 2;
		val = val - half;

		int rem = remaining();
		boolean neg = val < 0;

		int abs;
		if (neg) {
			abs = Math.abs(val);

			if (rem > abs) {
				value.getSecond().set(rem - abs);
				val = 0;
			} else {
				value.getSecond().set(0);
				val += rem;
			}

			return half - val;
		} else {
			abs = Math.abs(rem);

			if (abs > val) {
				value.getSecond().set(rem + val);
				val = 0;
			} else {
				value.getSecond().set(0);
				val += rem;
			}

			return half + val;
		}
	}

	public int slice() {
		int n = (int) (value.getSecond().get() * dpt);
		value.getSecond().getAndUpdate(i -> pos ? Math.max(0, i - n) : Math.min(i + n, 0));

		return n;
	}
}
