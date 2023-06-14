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

import java.awt.image.BufferedImage;

public enum Quality {
	NORMAL, FINE, POLISHED, FLAWLESS;

	public BufferedImage getOverlay() {
		if (ordinal() == 0) return null;

		return IO.getResourceAsImage("kawaipon/quality/" + name().toLowerCase() + ".png");
	}

	public static Quality get(double quality) {
		return values()[(int) Math.min(quality * values().length() / 20, 1)];
	}
}
