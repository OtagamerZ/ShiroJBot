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

package com.kuuhaku.handlers.games.kawaigotchi.enums;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public enum Race {
	PORO(getAsset("kawaigotchi/sheet/snugget_sheet.png").getImage(), new int[]{128, 116}),
	AKITA(getAsset("kawaigotchi/sheet/snugget_sheet.png").getImage(), new int[]{128, 116}),
	SNUGGET(getAsset("kawaigotchi/sheet/snugget_sheet.png").getImage(), new int[]{132, 120});

	private final Image sheet;
	private final int[] size;

	Race(Image sheet, int[] size) {
		this.sheet = sheet;
		this.size = size;
	}

	public BufferedImage extract(Stance stance, int var) {
		BufferedImage bi = new BufferedImage(sheet.getWidth(null), sheet.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(sheet, 0, 0, null);
		g2d.dispose();

		return switch (stance) {
			case IDLE -> bi.getSubimage(size[0] * var, 0, size[0], size[1]);
			case SLEEPING -> bi.getSubimage(size[0] * var, size[1], size[0], size[1]);
			case SAD -> bi.getSubimage(size[0] * var, size[1] * 2, size[0], size[1]);
			case HAPPY -> bi.getSubimage(size[0] * var, size[1] * 3, size[0], size[1]);
			case ANGRY -> bi.getSubimage(size[0] * var, size[1] * 4, size[0], size[1]);
			case DEAD -> bi.getSubimage(size[0] * var, size[1] * 5, size[0], size[1]);
		};
	}

	public int[] getSize() {
		return size;
	}

	@Override
	public String toString() {
		return switch (this) {
			case PORO -> "Poro";
			case AKITA -> "Akita";
			case SNUGGET -> "Snugget";
		};
	}

	private static ImageIcon getAsset(String path) {
		return new ImageIcon(Objects.requireNonNull(Race.class.getClassLoader().getResource(path)));
	}
}
