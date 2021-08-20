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
import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

public enum FrameColor {
	PINK("A cor característica da Shiro, batalhe com estratégia e perspicácia!", null),
	PURPLE("A cor de Imanity, seja o representante da peça rei!", null),
	BLUE("A cor da sabedoria, conquiste seus inimigos com calma e precisão!", null),
	CYAN("A cor de Seiren, divirta-se encurralando seus oponentes!", null),
	GREEN("A cor da natureza, canalize o poder de Disboard no seu deck!", null),
	YELLOW("A cor de Werebeast, mostre o poder da tecnologia e das nekos!", null),
	RED("A cor do combate, mostre a dominância de suas invocações!", null),
	GREY("A cor neutra, lute para vencer e apenas vencer!", null);

	private final String description;
	private final Function<Account, Boolean> req;

	FrameColor(String description, Function<Account, Boolean> req) {
		this.description = description;
		this.req = req;
	}

	public Color getColor() {
		return switch (this) {
			case PINK -> new Color(232, 116, 188);
			case PURPLE -> new Color(174, 116, 232);
			case BLUE -> new Color(116, 126, 232);
			case CYAN -> new Color(116, 197, 232);
			case GREEN -> new Color(139, 232, 116);
			case YELLOW -> new Color(232, 222, 116);
			case RED -> new Color(232, 116, 116);
			case GREY -> new Color(190, 190, 190);
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
			case GREY -> "Cinza";
		};
	}
}
