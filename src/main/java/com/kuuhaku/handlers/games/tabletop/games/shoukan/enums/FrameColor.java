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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.records.CompletionState;
import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

public enum FrameColor {
	PINK("Batalhe com perspicácia com a cor característica da Shiro!", null),
	PURPLE("Lute para dominar o campo, dê checkmate com a peça rei!", null),
	BLUE("Seja sábio, preveja a estratégia de seus oponentes!", null),
	CYAN("Divirta-se enquanto destroi seus oponentes!", null),
	GREEN("Canalize a força da natureza e lute com a graça de um pássaro!", null),
	YELLOW("Alveje o ponto fraco de seus oponentes, mostre a inovação de seu deck!", null),
	RED("No massacre você floresce, leve o campo de batalha aos seus inimigos!", null),
	GRAY("Lute com frieza, calcule seus movimentos e desintegre seus oponentes!", null),

	LEGACY_PINK("Empoeirado e gasto, trazendo a perspicácia de um tempo distante.", null),
	LEGACY_PURPLE("Empoeirado e gasto, mostrando a dominação de um tempo distante.", null),
	LEGACY_BLUE("Empoeirado e gasto, meditando sobre a sabedoria de um tempo distante.", null),
	LEGACY_CYAN("Empoeirado e gasto, relembrando suas vitórias passadas.", null),
	LEGACY_GREEN("Empoeirado e gasto, mas verde como um grande pinheiro de outrora.", null),
	LEGACY_YELLOW("Empoeirado e gasto, com uma tecnologia a muito tempo perdida.", null),
	LEGACY_RED("Empoeirado e gasto, ainda sujo com o sangue de seus inimigos.", null),
	LEGACY_GRAY("Empoeirado e gasto, praticamente desbotado mas afiado.", null),

	RAINBOW("**(Complete 10 coleções cromadas)** Seja fabuloso, mostre a elegância de uma estratégia estonteante!",
			acc -> acc.getCompState().values().stream().filter(CompletionState::foil).count() >= 10),

	BLACK("**(???)** Lute nas sombras, apareça na hora menos esperada e torne-se o nêmesis de seus oponentes.",
			acc -> false);

	private final String description;
	private final Function<Account, Boolean> req;

	FrameColor(String description, Function<Account, Boolean> req) {
		this.description = description;
		this.req = req;
	}

	public Color getColor() {
		return switch (this) {
			case PINK, LEGACY_PINK -> new Color(232, 116, 188);
			case PURPLE, LEGACY_PURPLE -> new Color(174, 116, 232);
			case BLUE, LEGACY_BLUE -> new Color(116, 126, 232);
			case CYAN, LEGACY_CYAN -> new Color(116, 197, 232);
			case GREEN, LEGACY_GREEN -> new Color(139, 232, 116);
			case YELLOW, LEGACY_YELLOW -> new Color(232, 222, 116);
			case RED, LEGACY_RED -> new Color(232, 116, 116);
			case GRAY, LEGACY_GRAY -> new Color(190, 190, 190);

			case RAINBOW -> Helper.getRandomColor();
			case BLACK -> Color.BLACK;
		};
	}

	public Color getTextColor() {
		return switch (this) {
			case PINK, LEGACY_PINK,
					PURPLE, LEGACY_PURPLE,
					BLUE, LEGACY_BLUE,
					CYAN, LEGACY_CYAN,
					GREEN, LEGACY_GREEN,
					YELLOW, LEGACY_YELLOW,
					RED, LEGACY_RED,
					GRAY, LEGACY_GRAY,
					RAINBOW -> Color.BLACK;

			case BLACK -> Color.WHITE;
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
					RED, LEGACY_RED,
					GRAY, LEGACY_GRAY -> Color.BLACK;

			case RAINBOW -> Helper.getRandomColor();
			case BLACK -> Color.WHITE;
		};
	}

	public BufferedImage getFront(boolean desc) {
		return Helper.getResourceAsImage(this.getClass(), "shoukan/frames/front/" + name().toLowerCase(Locale.ROOT) + (desc ? "" : "_nodesc") + ".png");
	}

	public BufferedImage getBack(Account acc) {
		boolean trans = !acc.getUltimate().isBlank() && acc.getCompletion(acc.getUltimate()).any();
		BufferedImage cover = Helper.getResourceAsImage(this.getClass(), "shoukan/frames/back/" + name().toLowerCase(Locale.ROOT) + (trans ? "_t" : "") + ".png");
		assert cover != null;

		BufferedImage canvas = new BufferedImage(cover.getWidth(), cover.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = canvas.createGraphics();

		if (trans) {
			g2d.drawImage(CardDAO.getUltimate(acc.getUltimate()).drawCardNoBorder(), 15, 16, 195, 318, null);
		}

		g2d.drawImage(cover, 0, 0, null);

		return canvas;
	}

	public String getDescription() {
		return description;
	}

	public boolean canUse(Account acc) {
		return req == null || req.apply(acc);
	}

	public static FrameColor getByName(String name) {
		return Arrays.stream(values()).filter(fc -> Helper.equalsAny(name, fc.name(), fc.toString())).findFirst().orElse(null);
	}

	@Override
	public String toString() {
		return switch (this) {
			case PINK -> "Rosa";
			case PURPLE -> "Roxo";
			case BLUE -> "Azul";
			case CYAN -> "Ciano";
			case GREEN -> "Verde";
			case YELLOW -> "Amarelo";
			case RED -> "Vermelho";
			case GRAY -> "Cinza";

			case LEGACY_PINK -> "Legado Rosa";
			case LEGACY_PURPLE -> "Legado Roxo";
			case LEGACY_BLUE -> "Legado Azul";
			case LEGACY_CYAN -> "Legado Ciano";
			case LEGACY_GREEN -> "Legado Verde";
			case LEGACY_YELLOW -> "Legado Amarelo";
			case LEGACY_RED -> "Legado Vermelho";
			case LEGACY_GRAY -> "Legado Cinza";

			case RAINBOW -> "Arco-iris";
			case BLACK -> "Negro";
		} + " (`" + name().toLowerCase(Locale.ROOT) + "`)";
	}
}
