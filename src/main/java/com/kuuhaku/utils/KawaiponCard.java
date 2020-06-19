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

import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import static com.kuuhaku.utils.KawaiponRarity.*;

public enum KawaiponCard {
	//FIXME LEGENDARY
	@NonNls SORA("Sora", Constants.NO_GAME_NO_LIFE, LEGENDARY),
	@NonNls SHIRO("Shiro", Constants.NO_GAME_NO_LIFE, LEGENDARY),
	@NonNls AKAME("Akame", Constants.AKAME_GA_KILL, LEGENDARY),
	@NonNls ESDEATH("Esdeath", Constants.AKAME_GA_KILL, LEGENDARY),

	//FIXME ULTRA
	@NonNls SCHWI("Schwi Dola", Constants.NO_GAME_NO_LIFE, ULTRA_RARE),
	@NonNls RIKU("Riku Dola", Constants.NO_GAME_NO_LIFE, ULTRA_RARE),
	@NonNls IZUNA("Izuna Hatsuse", Constants.NO_GAME_NO_LIFE, ULTRA_RARE),
	@NonNls NAJENDA("Najenda", Constants.AKAME_GA_KILL, ULTRA_RARE),
	@NonNls TATSUMI("Tatsumi", Constants.AKAME_GA_KILL, ULTRA_RARE),
	@NonNls KUROME("Kurome", Constants.AKAME_GA_KILL, ULTRA_RARE),

	//FIXME RARE
	@NonNls STEPH("Stephanie Dola", Constants.NO_GAME_NO_LIFE, RARE),
	@NonNls TET("Tet", Constants.NO_GAME_NO_LIFE, RARE),
	@NonNls JIBRIL("Jibril", Constants.NO_GAME_NO_LIFE, RARE),
	@NonNls MINE("Mine", Constants.AKAME_GA_KILL, RARE),
	@NonNls SHEELE("Sheele", Constants.AKAME_GA_KILL, RARE),
	@NonNls LEONE("Leone", Constants.AKAME_GA_KILL, RARE),
	@NonNls HONEST("Honest", Constants.AKAME_GA_KILL, RARE),

	//FIXME UNCOMMON
	@NonNls FEEL("Feel Nirvalen", Constants.NO_GAME_NO_LIFE, UNCOMMON),
	@NonNls CHLAMMY("Chlammy Zell", Constants.NO_GAME_NO_LIFE, UNCOMMON),
	@NonNls LUBBOCK("Lubbock", Constants.AKAME_GA_KILL, UNCOMMON),
	@NonNls BULAT("Bulat", Constants.AKAME_GA_KILL, UNCOMMON),
	@NonNls SUSANOO("Susanoo", Constants.AKAME_GA_KILL, UNCOMMON),

	//FIXME COMMON
	@NonNls MAKOTO("Makoto Dola", Constants.NO_GAME_NO_LIFE, COMMON),
	@NonNls INO("Ino Hatsuse", Constants.NO_GAME_NO_LIFE, COMMON),
	@NonNls MIKO("Miko", Constants.NO_GAME_NO_LIFE, COMMON),
	@NonNls CHELSEA("Chelsea", Constants.AKAME_GA_KILL, COMMON),
	@NonNls ZANK("Zank", Constants.AKAME_GA_KILL, COMMON),
	@NonNls DAIDARA("Daidara", Constants.AKAME_GA_KILL, COMMON),
	@NonNls NYAU("Nyau", Constants.AKAME_GA_KILL, COMMON),
	@NonNls LIVER("Liver", Constants.AKAME_GA_KILL, COMMON),


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

	private static class Constants {
		@NonNls
		private static final String NO_GAME_NO_LIFE = "No Game No Life";
		@NonNls
		private static final String AKAME_GA_KILL = "Akame Ga Kill";
	}
}
