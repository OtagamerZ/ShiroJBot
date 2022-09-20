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
import org.apache.commons.lang3.ArrayUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Origin(Race major, Race... minor) {
	public Origin(List<Race> races) {
		this(races.get(0), races.get(1));
	}

	public Race synergy() {
		if (major == Race.NONE) return Race.NONE;

		return major.fuse(minor[0]);
	}

	public List<BufferedImage> images() {
		return new ArrayList<>() {{
			if (major != Race.NONE) {
				add(major.getImage());
			}
			for (Race r : minor) {
				add(r.getImage());
			}
			if (synergy() != Race.NONE) {
				add(synergy().getImage());
			}
		}};
	}

	@Override
	public Race major() {
		return Utils.getOr(major, Race.NONE);
	}

	public Race[] minor() {
		if (major == Race.NONE) return minor;
		else if (demon()) return ArrayUtils.add(minor, major);

		return minor;
	}

	public boolean isPure() {
		System.out.println(Arrays.toString(minor));
		return minor.length == 0;
	}

	public boolean hasMinor(Race race) {
		for (Race r : minor) {
			if (r.isRace(race)) return true;
		}

		return false;
	}

	public boolean demon() {
		return Utils.equalsAny(Race.DEMON, minor);
	}
}
