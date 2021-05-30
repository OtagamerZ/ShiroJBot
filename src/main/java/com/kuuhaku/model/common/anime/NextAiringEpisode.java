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

package com.kuuhaku.model.common.anime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class NextAiringEpisode {
	private long episode;
	private long airingAt;

	public long getEpisode() {
		return episode;
	}

	public void setEpisode(long value) {
		this.episode = value;
	}

	public long getAiringAt() {
		return airingAt;
	}

	public void setAiringAt(long value) {
		this.airingAt = value;
	}

	public ZonedDateTime getAiringAtDate() {
		return Instant.ofEpochMilli(airingAt).atZone(ZoneId.of("GMT-3"));
	}
}
