/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import java.util.Locale;

public enum BuffType {
	XP, CARD, DROP, FOIL;

	public static BuffType of(String name) {
		return switch (name.toUpperCase(Locale.ROOT)) {
			case "XP" -> XP;
			case "CARTA" -> CARD;
			case "DROP" -> DROP;
			case "CROMADA" -> FOIL;
			default -> throw new IllegalStateException("Unexpected value: " + name);
		};
	}
}
