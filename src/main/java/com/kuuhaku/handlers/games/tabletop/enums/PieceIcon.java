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

package com.kuuhaku.handlers.games.tabletop.enums;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public enum PieceIcon {
	CROSS(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/cross.png")))),
	CIRCLE(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/circle.png")))),
	MAN(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/man.png")))),
	CROWN(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/crown.png")))),
	PAWN(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/pawn.png")))),
	ROOK(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/rook.png")))),
	KNIGHT(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/knight.png")))),
	BISHOP(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/bishop.png")))),
	QUEEN(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/queen.png")))),
	KING(new ImageIcon(Objects.requireNonNull(PieceIcon.class.getClassLoader().getResource("pieces/king.png"))));

	private final ImageIcon icon;

	PieceIcon(ImageIcon icon) {
		this.icon = icon;
	}

	public BufferedImage render(boolean white) {
		BufferedImage bi = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(icon.getImage(), 0, 0, null);
		if (white) {
			for (int y = 0; y < 64; y++) {
				for (int x = 0; x < 64; x++) {
					Color rgb = new Color(bi.getRGB(x, y), true);
					int r = 255 - rgb.getRed();
					int g = 255 - rgb.getGreen();
					int b = 255 - rgb.getBlue();

					bi.setRGB(x, y, new Color(r, g, b, rgb.getAlpha()).getRGB());
				}
			}
		}
		g2d.dispose();

		return bi;
	}
}
