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

package com.kuuhaku.utils;

public abstract class Bit {
	public static int set(int bits, int index, boolean value) {
		return set(bits, index, Utils.toInt(value), 1);
	}

	public static int set(int bits, int index, int value, int size) {
		index *= size;
		if (!Utils.between(index, 0, Integer.SIZE)) return bits;

		int mask = ((1 << size) - 1) << index;
		return (bits & ~mask) | (value & mask) << index;
	}

	public static boolean on(int bits, int index) {
		return get(bits, index, 1) > 0;
	}

	public static boolean on(int bits, int index, int size) {
		return get(bits, index, size) > 0;
	}

	public static int get(int bits, int index, int size) {
		index *= size;
		if (!Utils.between(index, 0, Integer.SIZE)) return 0;

		int mask = (1 << size) - 1;
		return (bits >> index) & mask;
	}
}
