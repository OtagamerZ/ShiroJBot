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

package com.kuuhaku.model.records.embed;

import com.kuuhaku.utils.Helper;

import java.util.List;

public record Image(List<String> image, List<String> join, List<String> leave) {
	public String getRandomImage() {
		List<String> imgs = Helper.getOr(image, List.of());
		if (imgs.isEmpty()) return null;

		return Helper.getRandomEntry();
	}

	public String getRandomJoin() {
		if (join == null || join.isEmpty()) return getRandomImage();
		return Helper.getRandomEntry(join);
	}

	public String getRandomLeave() {
		if (leave == null || leave.isEmpty()) return getRandomImage();
		return Helper.getRandomEntry(leave);
	}
}
