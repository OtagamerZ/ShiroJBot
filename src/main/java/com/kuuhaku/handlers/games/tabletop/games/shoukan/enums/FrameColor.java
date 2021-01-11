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
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

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

	public BufferedImage getFront() {
		try {
			return ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/frames/card_front_" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage getBack(Account acc) {
		try {
			boolean withUlt = !acc.getUltimate().isBlank();
			BufferedImage cover = ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/frames/card_back_" + name().toLowerCase() + (withUlt ? "_t" : "") + ".png")));
			BufferedImage canvas = new BufferedImage(cover.getWidth(), cover.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = canvas.createGraphics();

			if (withUlt) {
				AnimeName an = AnimeName.valueOf(acc.getUltimate());
				g2d.drawImage(CardDAO.getUltimate(an).drawCardNoBorder(), 26, 43, 172, 268, null);
			}

			g2d.drawImage(cover, 0, 0, null);

			return canvas;
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage getFrontEquipment() {
		try {
			return ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/frames/card_front_equip_" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage getFrontArena() {
		try {
			return ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/frames/card_front_arena_" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
	}

	public BufferedImage getFrontSpell() {
		try {
			return ImageIO.read(Objects.requireNonNull(FrameColor.class.getClassLoader().getResourceAsStream("shoukan/frames/card_front_spell_" + name().toLowerCase() + ".png")));
		} catch (IOException e) {
			return null;
		}
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
