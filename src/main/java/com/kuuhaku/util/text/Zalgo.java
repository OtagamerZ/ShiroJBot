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

public class Zalgo {
	public static final Zalgo INSTANCE = new Zalgo();

	private static final char[] diacUp = {
			'\u030d', '\u030e', '\u0304', '\u0305', '\u033f', '\u0311', '\u0306', '\u0310',
			'\u0352', '\u0357', '\u0351', '\u0307', '\u0308', '\u030a', '\u0342', '\u0343',
			'\u0344', '\u034a', '\u034b', '\u034c', '\u0303', '\u0302', '\u030c', '\u0350',
			'\u0300', '\u0301', '\u030b', '\u030f', '\u0312', '\u0313', '\u0314', '\u033d',
			'\u0309', '\u0363', '\u0364', '\u0365', '\u0366', '\u0367', '\u0368', '\u0369',
			'\u036a', '\u036b', '\u036c', '\u036d', '\u036e', '\u036f', '\u033e', '\u035b',
			'\u0346', '\u031a'
	};

	private static final char[] diacMiddle = {
			'\u0315', '\u031b', '\u0340', '\u0341', '\u0358', '\u0321', '\u0322', '\u0327',
			'\u0328', '\u0334', '\u0335', '\u0336', '\u034f', '\u035c', '\u035d', '\u035e',
			'\u035f', '\u0360', '\u0362', '\u0338', '\u0337', '\u0361', '\u0489'
	};

	private static final char[] diacDown = {
			'\u0316', '\u0317', '\u0318', '\u0319', '\u031c', '\u031d', '\u031e', '\u031f',
			'\u0320', '\u0324', '\u0325', '\u0326', '\u0329', '\u032a', '\u032b', '\u032c',
			'\u032d', '\u032e', '\u032f', '\u0330', '\u0331', '\u0332', '\u0333', '\u0339',
			'\u033a', '\u033b', '\u033c', '\u0345', '\u0347', '\u0348', '\u0349', '\u034d',
			'\u034e', '\u0353', '\u0354', '\u0355', '\u0356', '\u0359', '\u035a', '\u0323'
	};

	private final int power;
	private final double variation;

	public Zalgo() {
		power = 3;
		variation = 0.5;
	}

	public Zalgo(int power, double variation) {
		this.power = power;
		this.variation = Calc.clamp(variation, 0, 1);
	}

	public String curse(String text, int iterations) {
		char[] chars = text.toCharArray();

		StringBuilder sb = new StringBuilder();
		for (char c : chars) {
			sb.append(c);

			int zu = Calc.rng((int) (power * variation), power);
			for (int i = 0; i < zu; i++) {
				sb.append(Utils.getRandomEntry(diacUp));
			}

			int zm = Calc.rng((int) (power * variation), power);
			for (int i = 0; i < zm; i++) {
				sb.append(Utils.getRandomEntry(diacMiddle));
			}

			int zd = Calc.rng((int) (power * variation), power);
			for (int i = 0; i < zd; i++) {
				sb.append(Utils.getRandomEntry(diacDown));
			}
		}

		if (iterations <= 0) {
			return sb.toString();
		} else {
			return curse(sb.toString(), iterations - 1);
		}
	}
}