/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.util.Utils;

import java.awt.*;
import java.util.List;

public record Embed(Title title,
					Author author,
					String color,
					String body,
					String thumbnail,
					Image image,
					Boolean showDate,
					Footer footer,
					List<Field> fields
) {

	public Color getParsedColor() {
		try {
			return color == null ? null : Color.decode(color);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public Boolean showDate() {
		return Utils.getOr(showDate, false);
	}
}