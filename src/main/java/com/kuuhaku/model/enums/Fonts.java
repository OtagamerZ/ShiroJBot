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

package com.kuuhaku.model.enums;

import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.io.IOException;

public enum Fonts {
	DJB_GET_DIGITAL("font/DJBGetDigital.ttf"),
	DOREKING("font/Doreking.ttf"),
	HAMMERSMITH_ONE("font/HammersmithOne.ttf");

	private final Font font;

	Fonts(String path) {
		Font temp;

		try {
			temp = Font.createFont(Font.TRUETYPE_FONT, Helper.getResourceAsStream(this.getClass(), path));
		} catch (FontFormatException | IOException e) {
			temp = new java.awt.Font("Arial", Font.PLAIN, 11);
		}

		this.font = temp;
	}

	public Font getFont() {
		return font;
	}

	public Font deriveFont(int style, float size) {
		return font.deriveFont(style, size);
	}
}
