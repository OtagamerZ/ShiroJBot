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
	SORA("Sora", "No Game No Life", LEGENDARY),
	SHIRO("Shiro", "No Game No Life", LEGENDARY),
	AKAME("Akame", "Akame Ga Kill", LEGENDARY),
	ESDEATH("Esdeath", "Akame Ga Kill", LEGENDARY),

	//FIXME ULTRA
	SCHWI("Schwi Dola", "No Game No Life", ULTRA_RARE),
	RIKU("Riku Dola", "No Game No Life", ULTRA_RARE),
	IZUNA("Izuna Hatsuse", "No Game No Life", ULTRA_RARE),
	NAJENDA("Najenda", "Akame Ga Kill", ULTRA_RARE),
	TATSUMI("Tatsumi", "Akame Ga Kill", ULTRA_RARE),
	KUROME("Kurome", "Akame Ga Kill", ULTRA_RARE),

	//FIXME RARE
	STEPH("Stephanie Dola", "No Game No Life", RARE),
	TET("Tet", "No Game No Life", RARE),
	JIBRIL("Jibril", "No Game No Life", RARE),
	MINE("Mine", "Akame Ga Kill", RARE),
	SHEELE("Sheele", "Akame Ga Kill", RARE),
	LEONE("Leone", "Akame Ga Kill", RARE),
	HONEST("Honest", "Akame Ga Kill", RARE),

	//FIXME UNCOMMON
	FEEL("Feel Nirvalen", "No Game No Life", UNCOMMON),
	CHLAMMY("Chlammy Zell", "No Game No Life", UNCOMMON),
	LUBBOCK("Lubbock", "Akame Ga Kill", UNCOMMON),
	BULAT("Bulat", "Akame Ga Kill", UNCOMMON),
	SUSANOO("Susanoo", "Akame Ga Kill", UNCOMMON),

	//FIXME COMMON
	MAKOTO("Makoto Dola", "No Game No Life", COMMON),
	INO("Ino Hatsuse", "No Game No Life", COMMON),
	MIKO("Miko", "No Game No Life", COMMON),
	CHELSEA("Chelsea", "Akame Ga Kill", COMMON),
	ZANK("Zank", "Akame Ga Kill", COMMON),
	DAIDARA("Daidara", "Akame Ga Kill", COMMON),
	NYAU("Nyau", "Akame Ga Kill", COMMON),
	LIVER("Liver", "Akame Ga Kill", COMMON),


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

			Graphics2D g2d = frame.createGraphics();
			g2d.drawImage(card, 10, 10, null);
			g2d.drawImage(frame, 0, 0, null);

			g2d.dispose();

			return card;
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}
}
