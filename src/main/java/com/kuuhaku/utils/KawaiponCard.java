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

package com.kuuhaku.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import static com.kuuhaku.utils.KawaiponRarity.*;

public enum KawaiponCard {
	//FIXME LEGENDARY
	/*NGNL*/ SORA("Sora", "No Game No Life", LEGENDARY),
	/*NGNL*/ SHIRO("Shiro", "No Game No Life", LEGENDARY),

	//FIXME ULTRA
	/*NGNL*/ SCHWI("Schwi Dola", "No Game No Life", ULTRA_RARE),
	/*NGNL*/ RIKU("Riku Dola", "No Game No Life", ULTRA_RARE),
	/*NGNL*/ IZUNA("Izuna Hatsuse", "No Game No Life", ULTRA_RARE),

	//FIXME RARE
	/*NGNL*/ STEPH("Stephanie Dola", "No Game No Life", RARE),
	/*NGNL*/ TET("Tet", "No Game No Life", RARE),
	/*NGNL*/ JIBRIL("Jibril", "No Game No Life", RARE),

	//FIXME UNCOMMON
	/*NGNL*/ FEEL("Feel Nirvalen", "No Game No Life", UNCOMMON),
	/*NGNL*/ CHLAMMY("Chlammy Zell", "No Game No Life", UNCOMMON),

	//FIXME COMMON
	/*NGNL*/ MAKOTO("Makoto Dola", "No Game No Life", COMMON),
	/*NGNL*/ INO("Ino Hatsuse", "No Game No Life", COMMON),
	/*NGNL*/ MIKO("Miko", "No Game No Life", COMMON),

	;
	private final String name;
	private final String anime;
	private final KawaiponRarity rarity;

	KawaiponCard(String name, String anime, KawaiponRarity rarity) {
		this.name = name;
		this.anime = anime;
		this.rarity = rarity;
	}

	public String getName() {
		return name;
	}

	public String getAnime() {
		return anime;
	}

	public KawaiponRarity getRarity() {
		return rarity;
	}

	public BufferedImage getCard() {
		try {
			BufferedImage card = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/cards/" + name().toLowerCase() + ".jpg")));
			BufferedImage frame = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/frames/" + rarity.name().toLowerCase() + ".png")));

			Graphics2D g2d = card.createGraphics();
			g2d.drawImage(frame, 0, 0, null);

			g2d.dispose();

			return card;
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}
}
