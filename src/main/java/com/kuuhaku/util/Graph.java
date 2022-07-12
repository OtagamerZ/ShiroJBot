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

package com.kuuhaku.util;

import com.kuuhaku.exceptions.InvalidValueException;
import org.apache.logging.log4j.util.TriConsumer;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.DoubleStream;

public abstract class Graph {
	public static void drawOutlinedString(Graphics2D g2d, String text, int x, int y, float width, Color color) {
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
		drawMultilineString(g2d, text, x, y, width, 0, Function.identity(), g2d::drawString);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, int kerning) {
		drawMultilineString(g2d, text, x, y, width, kerning, Function.identity(), g2d::drawString);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, Function<String, String> processor) {
		drawMultilineString(g2d, text, x, y, width, 0, processor, g2d::drawString);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, int kerning, Function<String, String> processor) {
		drawMultilineString(g2d, text, x, y, width, kerning, processor, g2d::drawString);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, TriConsumer<String, Integer, Integer> renderer) {
		drawMultilineString(g2d, text, x, y, width, 0, Function.identity(), renderer);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, int kerning, TriConsumer<String, Integer, Integer> renderer) {
		drawMultilineString(g2d, text, x, y, width, kerning, Function.identity(), renderer);
	}

	public static void drawMultilineString(Graphics2D g2d, String text, int x, int y, int width, int kerning, Function<String, String> processor, TriConsumer<String, Integer, Integer> renderer) {
		String[] lines = text.split("\n");
		for (String line : lines) {
			String[] words = line.split("(?<=\\S[ \u200B])");
			int offset = 0;
			for (String s : words) {
				String word = processor.apply(s);
				FontMetrics m = g2d.getFontMetrics();

				if (offset + m.stringWidth(word) <= width) {
					renderer.accept(word, x + offset, y);
					offset += m.stringWidth(word);
				} else {
					y += m.getHeight() - kerning;
					renderer.accept(word, x, y);
					offset = m.stringWidth(word);
				}
			}

			y += g2d.getFontMetrics().getHeight() - kerning;
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

	public static BufferedImage toColorSpace(BufferedImage in, int type) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), type);
		Graphics2D g2d = out.createGraphics();
		g2d.drawImage(in, 0, 0, null);
		g2d.dispose();
		return out;
	}

	public static void forEachPixel(BufferedImage bi, TriConsumer<Integer, Integer, Integer> act) {
		int x;
		int y;
		int i = 0;
		while (true) {
			x = i % bi.getWidth();
			y = i / bi.getWidth();

			if (x >= bi.getWidth() || y >= bi.getHeight()) break;

			act.accept(x, y, bi.getRGB(x, y));
			i++;
		}
	}

	public static int[] unpackRGB(int rgb) {
		return new int[]{
				(rgb >> 24) & 0xFF,
				(rgb >> 16) & 0xFF,
				(rgb >> 8) & 0xFF,
				rgb & 0xFF
		};
	}

	public static int packRGB(int[] argb) {
		return packRGB(argb[0], argb[1], argb[2], argb[3]);
	}

	public static int packRGB(int a, int r, int g, int b) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel) {
		applyMask(source, mask, channel, false);
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel, boolean hasAlpha) {
		BufferedImage newMask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = newMask.createGraphics();
		g2d.drawImage(mask, 0, 0, newMask.getWidth(), newMask.getHeight(), null);
		g2d.dispose();

		forEachPixel(source, (x, y, rgb) -> {
			int[] color = unpackRGB(source.getRGB(x, y));

			int fac;
			if (hasAlpha) {
				fac = Math.min(color[0], unpackRGB(newMask.getRGB(x, y))[channel + 1]);
			} else
				fac = unpackRGB(newMask.getRGB(x, y))[channel + 1];
			source.setRGB(
					x,
					y,
					packRGB(fac, color[1], color[2], color[3])
			);
		});
	}

	public static void overlay(BufferedImage source, BufferedImage overlay) {
		if (overlay == null) return;

		Graphics2D g2d = source.createGraphics();
		g2d.drawImage(overlay,
				source.getWidth() / 2 - overlay.getWidth() / 2,
				source.getHeight() / 2 - overlay.getHeight() / 2,
				null
		);
		g2d.dispose();
	}

	public static Color adjust(Color in, int hue, int sat, int brightness) {
		int[] rgb = unpackRGB(in.getRGB());

		float[] hsv = Color.RGBtoHSB(rgb[1], rgb[2], rgb[3], null);
		hsv[0] = ((hsv[0] * 360 + hue) % 360) / 360;
		hsv[1] = Calc.clamp(sat / 100f, 0, 1);
		hsv[2] = Calc.clamp(brightness / 100f, 0, 1);

		return new Color(Color.getHSBColor(hsv[0], hsv[1], hsv[2]).getRGB());
	}
}
