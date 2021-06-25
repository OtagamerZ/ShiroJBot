/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

public enum IDKind {
	YOUTUBE_PLAYLIST, YOUTUBE_VIDEO;

	public String toValue() {
		return switch (this) {
			case YOUTUBE_PLAYLIST -> "youtube#playlist";
			case YOUTUBE_VIDEO -> "youtube#video";
		};
	}

	public static IDKind forValue(String value) throws IOException {
		if (value.equals("youtube#playlist")) return YOUTUBE_PLAYLIST;
		if (value.equals("youtube#video")) return YOUTUBE_VIDEO;
		throw new IOException("Cannot deserialize IDKind");
	}
}

