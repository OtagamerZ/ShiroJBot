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

package com.kuuhaku.model.records.youtube.enums;

import java.io.IOException;

public enum ItemKind {
	YOUTUBE_SEARCH_RESULT;

	public String toValue() {
		if (this == ItemKind.YOUTUBE_SEARCH_RESULT) {
			return "youtube#searchResult";
		}

		return null;
	}

	public static ItemKind forValue(String value) throws IOException {
		if (value.equals("youtube#searchResult")) return YOUTUBE_SEARCH_RESULT;
		throw new IOException("Cannot deserialize ItemKind");
	}
}
