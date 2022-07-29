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

package com.kuuhaku.util.text;

import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Zalgo {
	public static final Zalgo INSTANCE = new Zalgo();

	private static final Integer[] diacratic = Stream.of(
					IntStream.rangeClosed(0x0300, 0x036F),
					IntStream.rangeClosed(0x20D0, 0x20DF),
					IntStream.of(0x20E5, 0x20E6)
			)
			.flatMapToInt(Function.identity())
			.boxed()
			.toArray(Integer[]::new);

	private final int power;
	private final double variation;

	public Zalgo() {
		power = 20;
		variation = 0.5;
	}

	public Zalgo(int power, double variation) {
		this.power = power;
		this.variation = Calc.clamp(variation, 0, 1);
	}

	public String curse(String text) {
		char[] chars = text.toCharArray();

		StringBuilder sb = new StringBuilder();
		for (char c : chars) {
			sb.append(c);

			int zc = ((int) Calc.rng(power * variation, power));
			for (int i = 0; i < zc; i++) {
				sb.append(Utils.getRandomEntry(diacratic));
			}
		}

		return sb.toString();
	}
}
