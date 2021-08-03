/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.enums;

import java.util.Calendar;

public enum Event {
	BIRTHDAY, EASTER, HALLOWEEN, BLACKFRIDAY, XMAS, NONE;

	public static Event getCurrent() {
		Calendar c = Calendar.getInstance();

		return switch (c.get(Calendar.MONTH)) {
			case Calendar.APRIL -> {
				if (c.get(Calendar.DAY_OF_MONTH) == 29) yield BIRTHDAY;
				else yield EASTER;
			}
			case Calendar.OCTOBER -> HALLOWEEN;
			case Calendar.NOVEMBER -> {
				if (c.get(Calendar.WEEK_OF_MONTH) == 4) yield BLACKFRIDAY;
				else yield NONE;
			}
			case Calendar.DECEMBER -> XMAS;
			default -> NONE;
		};
	}
}
