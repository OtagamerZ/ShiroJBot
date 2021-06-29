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

package com.kuuhaku.model.records.anime;

import com.kuuhaku.utils.Helper;

import java.util.List;

public record Media(
		Long idMal,
		Title title,
		String status,
		StartDate startDate,
		Long episodes,
		CoverImage coverImage,
		List<String> genres,
		Long averageScore,
		Long popularity,
		Studios studios,
		Staff staff,
		NextAiringEpisode nextAiringEpisode,
		Trailer trailer,
		String description
) {

	@Override
	public Long episodes() {
		if (nextAiringEpisode != null)
			return Helper.getOr(episodes, nextAiringEpisode.episode() - 1);
		else return episodes;
	}

	@Override
	public Long idMal() {
		return Helper.getOr(idMal, 0L);
	}

	@Override
	public Long popularity() {
		return Helper.getOr(popularity, 0L);
	}

	@Override
	public Long averageScore() {
		return Helper.getOr(averageScore, 0L);
	}
}
