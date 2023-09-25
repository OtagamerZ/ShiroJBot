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

package com.kuuhaku.model.enums;

import com.kuuhaku.util.IO;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.awt.font.TextAttribute.*;

public enum Fonts {
	UBUNTU_MONO("font/UbuntuMono.ttf"),
	OPEN_SANS("font/OpenSans.ttf"),
	OPEN_SANS_BOLD("font/OpenSans-Bold.ttf"),
	OPEN_SANS_EXTRABOLD("font/OpenSans-ExtraBold.ttf"),
	OPEN_SANS_COMPACT("font/OpenSans-Compact.ttf"),
	NOTO_SANS("font/NotoSansJP.otf"),
	DOREKING("font/Doreking.ttf"),
	DEFAULT("");

	private final Font font;

	Fonts(String path) {
		Font temp;

		if (path.isBlank()) {
			temp = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
		} else {
			try (InputStream is = IO.getResourceAsStream(path)) {
				temp = Font.createFont(Font.TRUETYPE_FONT, is);
			} catch (FontFormatException | IOException e) {
				temp = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
			}
		}

		this.font = temp;
	}

	public Font getFont() {
		return font;
	}

	public Font deriveFont(int style, float size) {
		return deriveFont(size, 1, false);
	}

	public Font deriveFont(float size, float weight, boolean italic) {
		return font.deriveFont(Map.of(
				SIZE, size,
				WEIGHT, weight * size,
				POSTURE, italic ? POSTURE_OBLIQUE : POSTURE_REGULAR
		));
	}
}
