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

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;

public enum FrameColor {
	PINK("A cor característica da Shiro, batalhe com estratégia e perspicácia!"),
	PURPLE("A cor de Imanity, seja o representante da peça rei!"),
	BLUE("A cor da sabedoria, conquiste seus inimigos com calma e precisão!"),
	CYAN("A cor de Seiren, divirta-se encurralando seus oponentes!"),
	GREEN("A cor da natureza, canalize o poder de Disboard no seu deck!"),
	YELLOW("A cor de Werebeast, mostre o poder da tecnologia e das nekos!"),
	RED("A cor do combate, mostre a dominância de suas invocações!"),
	GREY("A cor neutra, lute para vencer e apenas vencer!");

	private final String description;

	FrameColor(String description) {
		this.description = description;
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

	public BufferedImage getFront() {
		return Helper.getResourceAsImage(this.getClass(), "shoukan/frames/card_front_" + name().toLowerCase(Locale.ROOT) + ".png");
	}

	public BufferedImage getBack(Account acc) {
		boolean withUlt = !acc.getUltimate().isBlank() && CardDAO.hasCompleted(acc.getUid(), acc.getUltimate(), false);
		BufferedImage cover = Helper.getResourceAsImage(this.getClass(), "shoukan/frames/card_back_" + name().toLowerCase(Locale.ROOT) + (withUlt ? "_t" : "") + ".png");
		assert cover != null;
		BufferedImage canvas = new BufferedImage(cover.getWidth(), cover.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = canvas.createGraphics();

		if (withUlt) {
			g2d.drawImage(CardDAO.getUltimate(acc.getUltimate()).drawCardNoBorder(), 26, 43, 172, 268, null);
		}

		g2d.drawImage(cover, 0, 0, null);

		return canvas;
	}

	public BufferedImage getFrontEquipment() {
		return Helper.getResourceAsImage(this.getClass(), "shoukan/frames/card_front_equip_" + name().toLowerCase(Locale.ROOT) + ".png");
	}

	public BufferedImage getFrontArena() {
		return Helper.getResourceAsImage(this.getClass(), "shoukan/frames/card_front_arena_" + name().toLowerCase(Locale.ROOT) + ".png");
	}

	public BufferedImage getFrontSpell() {
		return Helper.getResourceAsImage(this.getClass(), "shoukan/frames/card_front_spell_" + name().toLowerCase(Locale.ROOT) + ".png");
	}

	public String getDescription() {
		return description;
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
