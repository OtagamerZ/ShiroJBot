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

package com.kuuhaku.handlers.games.tabletop.framework;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public abstract class Piece {
	private final String ownerId;
	private final boolean white;
	private final String icon;

	public Piece(String ownerId, boolean white, String icon) {
		this.ownerId = ownerId;
		this.white = white;
		this.icon = icon;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public boolean isWhite() {
		return white;
	}

	public String getIconPath() {
		return icon;
	}

	public Image getIcon() {
		URL path = this.getClass().getClassLoader().getResource(icon);
		assert path != null;

		BufferedImage bi = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(new ImageIcon(path).getImage(), 0, 0, null);
		if (white) {
			for (int y = 0; y < 64; y++) {
				for (int x = 0; x < 64; x++) {
					int[] rgb = Helper.unpackRGB(bi.getRGB(x, y));
					int r = 255 - rgb[1];
					int g = 255 - rgb[2];
					int b = 255 - rgb[3];

					bi.setRGB(
							x,
							y,
							Helper.packRGB(rgb[0], r, g, b)
					);
				}
			}
		}
		g2d.dispose();

		return bi;
	}
}
