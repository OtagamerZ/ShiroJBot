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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.util.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public record Origin(Race major, Race minor) {
	public Origin(List<Race> races) {
		this(races.get(0), races.get(1));
	}

	public Race synergy() {
		return major.fuse(minor);
	}

	public List<BufferedImage> images() {
		return new ArrayList<>() {{
			add(major.getImage());
			add(minor.getImage());
			add(synergy().getImage());
		}};
	}

	@Override
	public Race major() {
		return Utils.getOr(major, Race.NONE);
	}

	@Override
	public Race minor() {
		return demon() ? major : Utils.getOr(minor, Race.NONE);
	}

	public boolean demon() {
		return minor == Race.DEMON;
	}
}
