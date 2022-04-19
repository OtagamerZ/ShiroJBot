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
	public static int set(int flags, int index, boolean value) {
		if (!Utils.between(index, 0, 32)) return flags;

		if (value) {
			return flags |= 1 << index;
		} else {
			return flags &= ~(1 << index);
		}
	}

	public static boolean get(int flags, int index) {
		if (!Utils.between(index, 0, 32)) return false;

		return ((flags >> index) & 1) == 1;
	}
}
