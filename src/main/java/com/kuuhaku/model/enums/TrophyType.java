/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public enum TrophyType {
	SHOUKAN_1("Shoukan 1ª Temporada", "Troféu de campeão da primeira temporada competitiva de Shoukan");

	private final String name;
	private final String description;

	TrophyType(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BufferedImage getImage() {
		try {
			return ImageIO.read(Objects.requireNonNull(TrophyType.class.getClassLoader().getResourceAsStream("shoukan/trophy/" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}
}
