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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public enum Race {
	HUMAN,
	ELF,
	BESTIAL,
	UNDEAD,
	MACHINE,
	DIVINITY,
	MYSTICAL,
	CREATURE,
	SPIRIT,
	DEMON;

	public BufferedImage getIcon() {
		try {
			return ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/race/" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return switch (this) {
			case HUMAN -> "Humano";
			case ELF -> "Elfo";
			case BESTIAL -> "Bestial";
			case UNDEAD -> "Morto-vivo";
			case MACHINE -> "Máquina";
			case DIVINITY -> "Divindade";
			case MYSTICAL -> "Místico";
			case CREATURE -> "Criatura";
			case SPIRIT -> "Espírito";
			case DEMON -> "Demônio";
		};
	}
}
