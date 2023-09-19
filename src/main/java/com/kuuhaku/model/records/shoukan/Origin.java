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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.util.Utils;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Origin(boolean variant, Race major, Race... minor) {
	public List<BufferedImage> images() {
		List<BufferedImage> out = new ArrayList<>();

		if (major != Race.NONE) {
			out.add(major.getImage());
		}
		for (Race r : minor) {
			out.add(r.getImage());
		}
		if (synergy() != Race.NONE) {
			out.add(synergy().getImage());
		}

		return out;
	}

	@Override
	public Race major() {
		return Utils.getOr(major, Race.NONE);
	}

	public Race[] minor() {
		if (major == Race.MIXED) return minor;
		else if (demon()) return ArrayUtils.add(minor, major);

		return minor;
	}

	public Race synergy() {
		if (major == Race.NONE || minor.length == 0) return Race.NONE;

		Race r = major.fuse(minor[0]);
		return variant ? r.getVariant() : r;
	}

	public boolean isPure() {
		return major != Race.NONE && minor.length == 0;
	}

	public boolean hasMinor(Race race) {
		for (Race r : minor()) {
			if (r.isRace(race)) return true;
		}

		return false;
	}

	public boolean demon() {
		return Utils.equalsAny(Race.DEMON, minor);
	}

	@Override
	public String toString() {
		return "{ major: " + major + ", minor: " + Arrays.toString(minor) + " }";
	}
}
