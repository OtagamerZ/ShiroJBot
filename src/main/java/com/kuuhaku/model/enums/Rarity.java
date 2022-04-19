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

package com.kuuhaku.model.enums;

import com.kuuhaku.utils.IO;

import java.awt.*;

public enum Rarity {
	COMMON(1, 0xFFFFFF),
	UNCOMMON(2, 0x03BB85),
	RARE(3, 0x70D1F4),
	ULTRA_RARE(4, 0x9966CC),
	LEGENDARY(5, 0xDC9018),
	ULTIMATE(-1, 0xD400AA),
	EVOGEAR(-1, 0x0),
	FIELD(-1, 0x0),
	FUSION(-1, 0x0);

	private final int index;
	private final int color;

	Rarity(int index, int color) {
		this.index = index;
		this.color = color;
	}

	public int getIndex() {
		return index;
	}

	public Color getColor(boolean foil) {
		int color = this.color;
		if (foil) {
			int[] rgb = IO.unpackRGB(color);

			float[] hsv;
			hsv = Color.RGBtoHSB(rgb[1], rgb[2], rgb[3], null);
			hsv[0] = ((hsv[0] * 360 + 180) % 360) / 360;

			rgb = IO.unpackRGB(Color.getHSBColor(hsv[0], hsv[1], hsv[2]).getRGB());
			color = IO.packRGB(255, rgb[1], rgb[2], rgb[3]);
		}

		return new Color(color);
	}
}
