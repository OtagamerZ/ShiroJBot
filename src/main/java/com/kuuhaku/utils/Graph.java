/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

public abstract class Graph {
	public static void drawOutlinedString(Graphics2D g2d, String text, int x, int y, int width, Color color) {
		Stroke origStroke = g2d.getStroke();
		Color origColor = g2d.getColor();

		TextLayout layout = new TextLayout(text, g2d.getFont(), g2d.getFontRenderContext());
		Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, y));

		g2d.setStroke(new BasicStroke(width));
		g2d.setColor(color);
		g2d.draw(outline);

		g2d.setStroke(origStroke);
		g2d.setColor(origColor);

		g2d.fill(outline);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width) {
		FontMetrics m = g2d.getFontMetrics();

		String[] lines = text.split("\n");
		for (String line : lines) {
			if (m.stringWidth(line) <= width) {
				g2d.drawString(line, x, y);
				y += m.getHeight();
			} else {
				String[] words = text.split("\\s+");
				StringBuilder sb = new StringBuilder(words[0]);
				for (int i = 1; i < words.length; i++) {
					if (m.stringWidth(sb + words[i]) <= width) {
						sb.append(" ").append(words[i]);
					} else {
						g2d.drawString(sb.toString(), x, y);
						sb.setLength(0);
						sb.append(words[i]);
						y += m.getHeight();
					}
				}

				if (!sb.isEmpty()) {
					g2d.drawString(sb.toString(), x, y);
				}
			}
		}
	}
}
