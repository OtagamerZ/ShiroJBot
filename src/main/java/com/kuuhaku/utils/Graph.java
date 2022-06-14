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

import com.kuuhaku.exceptions.InvalidValueException;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;

public abstract class Graph {
	public static void drawOutlinedString(Graphics2D g2d, String text, int x, int y, int width, Color color) {
		Stroke origStroke = g2d.getStroke();
		Color origColor = g2d.getColor();

		TextLayout layout = new TextLayout(text, g2d.getFont(), g2d.getFontRenderContext());
		Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, y));

		g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, null, 0));
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

	public static void applyTransformed(Graphics2D g2d, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, 0, new Point(), 1, action);
	}

	public static void applyTransformed(Graphics2D g2d, int x, int y, Consumer<Graphics2D> action) {
		applyTransformed(g2d, x, y, 0, new Point(), 1, action);
	}

	public static void applyTransformed(Graphics2D g2d, double ang, Point axis, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, ang, axis, 1, action);
	}

	public static void applyTransformed(Graphics2D g2d, double scale, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, 0, new Point(), scale, action);
	}

	public static void applyTransformed(Graphics2D g2d, int x, int y, double ang, Point axis, double scale, Consumer<Graphics2D> action) {
		AffineTransform trans = g2d.getTransform();
		g2d.translate(x, y);
		g2d.rotate(Math.toRadians(ang), axis.x, axis.y);
		g2d.scale(scale, scale);
		action.accept((Graphics2D) g2d.create());
		g2d.setTransform(trans);
	}

	public static Polygon makePoly(int... xy) {
		if (xy.length % 2 != 0) throw new InvalidValueException("Supplied coordinate count must be even.");

		Polygon poly = new Polygon();
		for (int i = 0; i < xy.length; i += 2) {
			poly.addPoint(xy[i], xy[i + 1]);
		}

		return poly;
	}

	public static Polygon makePoly(Dimension size, double... xy) {
		AtomicInteger i = new AtomicInteger();

		return makePoly(DoubleStream.of(xy)
				.mapToInt(d -> (int) ((i.getAndIncrement() % 2 == 0 ? size.width : size.height) * d))
				.toArray());
	}
}
