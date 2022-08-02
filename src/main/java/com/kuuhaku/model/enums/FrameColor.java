/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

public enum FrameColor {
	PINK,
	PURPLE,
	BLUE,
	CYAN,
	GREEN,
	YELLOW,
	ORANGE,
	RED,
	GRAY,

	LEGACY_PINK(Account::isOldUser),
	LEGACY_PURPLE(Account::isOldUser),
	LEGACY_BLUE(Account::isOldUser),
	LEGACY_CYAN(Account::isOldUser),
	LEGACY_GREEN(Account::isOldUser),
	LEGACY_YELLOW(Account::isOldUser),
	LEGACY_RED(Account::isOldUser),
	LEGACY_GRAY(Account::isOldUser),

	/*RAINBOW("**(Complete 10 coleções cromadas)** Seja fabuloso, mostre a elegância de uma estratégia estonteante!",
			acc -> acc.getCompState().values().stream().filter(CompletionState::foil).count() >= 10),

	BLACK("**(Conquista \"O Intocável\")** Lute nas sombras, apareça na hora menos esperada e torne-se o nêmesis de seus oponentes.",
			acc -> acc.getAchievements().contains(Achievement.UNTOUCHABLE)),

	HALLOWEEN("**(Conquista \"Noites de Arrepio\")** Muahaha, invoque os espíritos malígnos para atormentar seus oponentes!",
			acc -> acc.getAchievements().contains(Achievement.SPOOKY_NIGHTS)),

	GLITCH("**(Emblema \"Bug hunter\")** Ę̶̄͛Ŗ̴̓R̸̩͉͗O̴̪͉͊:̸̻̗͗ ̶̧̤̋̕P̴̘̪͑R̶̳̭̈̂Ǫ̸͒̽T̷̡̗̈́̃Ǫ̶̨̈́̐C̸̯͛̂O̴̯̓L̶̲̱̾̌Ọ̸̗͑̓ ̷̰͓̅͌\"̶̝̈͝D̶̳̯̈́Ĕ̵͍Ŕ̴ͅR̶̮̹͛Õ̶̢̾T̶͓͆A̸͚̰͆\"̶̡̌̓ ̸̬̃̈́N̶̢͉̒Ã̸͍̀Ȍ̸̘ͅ ̵̥͒̈́E̵̤̹̽̅Ṅ̷̼̆C̸̞̒O̷͚̪̎Ň̵͎Ṱ̵̨̽R̸̘̍̆Ả̴̙̞͝D̵̜͍̈́̋O̵̯͆",
			acc -> Tag.getTags(Main.getMemberByID(acc.getUid())).contains(Tag.BUG_HUNTER)
	),

	PADORU("**(Emblema \"Padoru padoru\")** Hashiro sori yo, kaze no you ni, tsukimihara wo **PADORU PADORU!**",
			acc -> Tag.getTags(Main.getMemberByID(acc.getUid())).contains(Tag.PADORU_PADORU)
	),

	METALLIC("**(75% das conquistas desbloqueadas)** Com estilo (e um revestimento semi-transparente), faça suas jogadas mostrando sua classe!",
			acc -> (float) acc.getMedalBag().size() / Achievement.getMedalBag().size() > 0.75f
	),

	RICH("**(Emblema \"Rico\")** Uns chamam de playboy, outros de ganancioso, mas no fim todos querem um pedaço da grana!",
			acc -> Tag.getTags(Main.getMemberByID(acc.getUid())).contains(Tag.RICO)
	),*/
	;

	private final Function<Account, Boolean> req;

	FrameColor() {
		this.req = null;
	}

	FrameColor(Function<Account, Boolean> req) {
		this.req = req;
	}

	public Color getThemeColor() {
		return switch (this) {
			case PINK, LEGACY_PINK -> new Color(0xE874BC);
			case PURPLE, LEGACY_PURPLE -> new Color(0xAE74E8);
			case BLUE, LEGACY_BLUE -> new Color(0x747EE8);
			case CYAN, LEGACY_CYAN -> new Color(0x74C5E8);
			case GREEN, LEGACY_GREEN -> new Color(0x8BE874);
			case YELLOW, LEGACY_YELLOW -> new Color(0xE8DE74);
			case ORANGE -> new Color(0xF39549);
			case RED, LEGACY_RED -> new Color(0xE87474);
			case GRAY, LEGACY_GRAY -> new Color(0xBEBEBE);

			/*case RAINBOW, GLITCH -> ImageHelper.getRandomColor();
			case BLACK -> Color.BLACK;
			case HALLOWEEN -> new Color(220, 89, 16);
			case PADORU -> new Color(177, 30, 49);
			case METALLIC -> new Color(190, 194, 203);
			case RICH -> new Color(212, 175, 55);*/
		};
	}

	public Color getBackgroundColor() {
		return switch (this) {
			case PINK, LEGACY_PINK,
					PURPLE, LEGACY_PURPLE,
					BLUE, LEGACY_BLUE,
					CYAN, LEGACY_CYAN,
					GREEN, LEGACY_GREEN,
					YELLOW, LEGACY_YELLOW,
					ORANGE,
					RED, LEGACY_RED,
					GRAY, LEGACY_GRAY
					/*HALLOWEEN*/ -> Color.BLACK;

			/*case BLACK -> Color.WHITE;
			case RAINBOW -> ImageHelper.toLuma(getThemeColor().getRGB()) > 127 ? Color.BLACK : Color.WHITE;
			case GLITCH -> ImageHelper.reverseColor(getThemeColor());
			case PADORU, METALLIC, RICH -> getThemeColor().darker();*/
		};
	}

	public Color getPrimaryColor() {
		return switch (this) {
			case PINK, LEGACY_PINK,
					PURPLE, LEGACY_PURPLE,
					BLUE, LEGACY_BLUE,
					CYAN, LEGACY_CYAN,
					GREEN, LEGACY_GREEN,
					YELLOW, LEGACY_YELLOW,
					ORANGE,
					RED, LEGACY_RED,
					GRAY, LEGACY_GRAY
					/*HALLOWEEN, PADORU, METALLIC, RICH*/ -> Color.WHITE;

			/*case BLACK -> Color.BLACK;
			case RAINBOW, GLITCH -> getThemeColor();*/
		};
	}

	public Color getSecondaryColor() {
		return switch (this) {
			case PINK, LEGACY_PINK,
					PURPLE, LEGACY_PURPLE,
					BLUE, LEGACY_BLUE,
					CYAN, LEGACY_CYAN,
					GREEN, LEGACY_GREEN,
					YELLOW, LEGACY_YELLOW,
					ORANGE,
					RED, LEGACY_RED,
					GRAY, LEGACY_GRAY
					/*RAINBOW, METALLIC*/ -> Color.BLACK;

			//case BLACK, HALLOWEEN, GLITCH, PADORU, RICH -> Color.WHITE;
		};
	}

	public BufferedImage getFront(boolean desc) {
		return IO.getResourceAsImage("shoukan/frames/front/" + name().toLowerCase(Locale.ROOT) + (desc ? "" : "_nodesc") + ".png");
	}

	public BufferedImage getBack(Deck deck) {
		BufferedImage cover = IO.getResourceAsImage("shoukan/frames/back/" + name().toLowerCase(Locale.ROOT) + (deck.getCover() != null ? "_t" : "") + ".png");
		assert cover != null;

		BufferedImage canvas = new BufferedImage(cover.getWidth(), cover.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = canvas.createGraphics();

		if (deck.getCover() != null) {
			g2d.drawImage(deck.getCover().drawCardNoBorder(), 15, 16, 195, 318, null);
		}

		g2d.drawImage(cover, 0, 0, null);

		return canvas;
	}

	public String getName(I18N locale) {
		return locale.get("frame/" + name());
	}

	public String getDescription(I18N locale) {
		return locale.get("frame/" + name() + "_desc");
	}

	public boolean canUse(Account acc) {
		return req == null || req.apply(acc);
	}

	public static FrameColor getByName(String name) {
		return Arrays.stream(values()).filter(fc -> Utils.equalsAny(name, fc.name(), fc.toString())).findFirst().orElse(null);
	}

	public Shape getBoundary() {
		if (name().startsWith("LEGACY")) {
			return new Rectangle(225, 350);
		}

		return new Polygon(
				new int[]{1, 14, 211, 224, 224, 211, 14, 1},
				new int[]{14, 1, 1, 14, 336, 349, 349, 336},
				8
		);
	}

	public boolean isLegacy() {
		return name().startsWith("LEGACY");
	}

	/*
	@Override
	public String toString() {
		return switch (this) {
			case RAINBOW -> "Arco-iris";
			case BLACK -> "Negro";
			case HALLOWEEN -> "Halloween";
			case GLITCH -> "Glitch";

			case PADORU -> "Padoru";
			case METALLIC -> "Metálico";
			case RICH -> "Rico";
		} + " (`" + name().toLowerCase(Locale.ROOT) + "`)";
	}
	*/
}
