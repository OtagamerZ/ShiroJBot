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
import org.apache.logging.log4j.util.TriConsumer;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
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
		drawMultilineString(g2d, text, x, y, width, Function.identity());
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, Function<String, String> processor) {
		drawMultilineString(g2d, text, x, y, width, processor, g2d::drawString);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, Function<String, String> processor, TriConsumer<String, Integer, Integer> renderer) {
		FontMetrics m = g2d.getFontMetrics();

		String[] lines = text.split("\n");
		for (String line : lines) {
			String[] words = line.split("\\s+");
			int offset = 0;
			for (String s : words) {
				String word = processor.apply(s);
				if (offset + m.stringWidth(word) <= width) {
					renderer.accept(word, x + offset, y);
					offset += m.stringWidth(word + " ");
				} else {
					y += m.getHeight();
					renderer.accept(word, x, y);
					offset = m.stringWidth(word + " ");
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

	public static void applyTransformed(Graphics2D g, int x, int y, double ang, Point axis, double scale, Consumer<Graphics2D> action) {
		Graphics2D g2d = (Graphics2D) g.create();

		g2d.translate(x, y);
		g2d.rotate(Math.toRadians(ang), axis.x, axis.y);
		g2d.scale(scale, scale);
		action.accept(g2d);

		g2d.dispose();
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

	public static void drawOutlined(Graphics2D g, Shape shape, int width, Color color) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setStroke(new BasicStroke(1));
		g2d.fill(shape);

		g2d.setColor(color);
		g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.draw(shape);

		g2d.dispose();
	}

	public static Color withOpacity(Color in, float opacity) {
		opacity = Calc.clamp(opacity, 0, 1);

		return new Color(
				in.getRed(),
				in.getGreen(),
				in.getBlue(),
				(int) (255 * opacity)
		);
	}
}
