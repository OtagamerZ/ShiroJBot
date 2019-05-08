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
	COMMON(1, 0xFFFFFF, "<:common:726171819664736268>"),
	UNCOMMON(2, 0x03BB85, "<:uncommon:726171819400232962>"),
	RARE(3, 0x70D1F4, "<:rare:726171819853480007>"),
	ULTRA_RARE(4, 0x9966CC, "<:ultra_rare:726171819786240091>"),
	LEGENDARY(5, 0xDC9018, "<:legendary:726171819945623682>"),
	ULTIMATE(-1, 0xD400AA, ""),
	EVOGEAR(),
	FIELD(),
	FUSION();

	private final int index;
	private final int color;
	private final String emote;

	Rarity(int index, int color, String emote) {
		this.index = index;
		this.color = color;
		this.emote = emote;
	}

	Rarity() {
		this.index = -1;
		this.color = 0;
		this.emote = "";
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

	public String getEmote() {
		return emote;
	}
}
