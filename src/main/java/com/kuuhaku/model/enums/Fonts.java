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

import com.kuuhaku.util.IO;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.text.AttributedCharacterIterator.Attribute;

public enum Fonts {
	HAMMERSMITH_ONE("font/HammersmithOne.ttf"),
	STAATLICHES("font/Staatliches.ttf");

	private final Font font;

	Fonts(String path) {
		Font temp;

		try (InputStream is = IO.getResourceAsStream(path)) {
			temp = Font.createFont(Font.TRUETYPE_FONT, is);
		} catch (FontFormatException | IOException e) {
			temp = new Font("Arial", Font.PLAIN, 11);
		}

		this.font = temp;
	}

	public Font getFont() {
		return font;
	}

	public Font deriveFont(int style, float size) {
		return font.deriveFont(style, size);
	}

	public Font deriveFont(int style, float size, Map<? extends Attribute, ?> attributes) {
		return deriveFont(style, size).deriveFont(attributes);
	}
}