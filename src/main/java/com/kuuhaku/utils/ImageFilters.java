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

package com.kuuhaku.utils;

import com.kuuhaku.model.enums.PixelOp;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageFilters {
	public static BufferedImage noise(BufferedImage in) {
		BufferedImage source = IO.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		IO.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			out.setRGB(x, y, PixelOp.MULTIPLY.get(rgb, 0xFF000000 | Calc.rng(0, 0xFFFFFF)));
		});

		return out;
	}

	public static BufferedImage shift(BufferedImage in, int type) {
		BufferedImage source = IO.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		IO.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			try {
				if ((type == 0 ? y : type == 1 ? x : y + x) % 2 == 0) out.setRGB(x, y, 0);
				else out.setRGB(x, y, rgb);
			} catch (ArrayIndexOutOfBoundsException ignore) {
			}
		});

		return out;
	}

	public static BufferedImage mirror(BufferedImage in, int type) {
		BufferedImage source = IO.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		IO.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			int val = type > 1 ? y : x;
			int half = (type > 1 ? source.getHeight() : source.getWidth()) / 2;
			int i = half + (half - val - half % 2);
			int pos = switch (type) {
				case 0, 2 -> val > half ? i : val;
				case 1, 3 -> val < half ? i : val;
				default -> throw new IllegalStateException("Unexpected value: " + type);
			};

			try {
				out.setRGB(x, y, type > 1 ? source.getRGB(x, pos) : source.getRGB(pos, y));
			} catch (ArrayIndexOutOfBoundsException ignore) {
			}
		});

		return out;
	}

	public static BufferedImage glitch(BufferedImage in, int offset) {
		BufferedImage source = IO.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage[] layers = {
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB)
		};

		int diag = Calc.hip(offset, offset);
		IO.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			int[] colors = IO.unpackRGB(source.getRGB(x, y));
			int[] ext = new int[3];

			ext[0] = (colors[1] / 3) << 24 | colors[1] << 16;
			ext[1] = (colors[2] / 3) << 24 | colors[2] << 8;
			ext[2] = (colors[3] / 3) << 24 | colors[3];

			for (int j = 0; j < layers.length; j++) {
				try {
					switch (j) {
						case 0 -> layers[j].setRGB(x, y - offset, ext[j]);
						case 1 -> layers[j].setRGB(x - diag, y + diag, ext[j]);
						case 2 -> layers[j].setRGB(x + diag, y + diag, ext[j]);
					}
				} catch (ArrayIndexOutOfBoundsException ignore) {
				}
			}
		});

		IO.forEachPixel(out, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			int[] color = new int[3];
			for (int k = 0; k < layers.length; k++) {
				int c = layers[k].getRGB(x, y);
				color[k] = (c >> (16 - k * 8)) & 0xFF;
			}

			out.setRGB(x, y, IO.packRGB(0xFF, color[0], color[1], color[2]));
		});

		return out;
	}

	public static BufferedImage invert(BufferedImage in, boolean onlyHue) {
		BufferedImage source = IO.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		IO.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];
			int[] color = IO.unpackRGB(rgb);

			if (onlyHue) {
				float[] hsv;
				hsv = Color.RGBtoHSB(color[1], color[2], color[3], null);
				hsv[0] = ((hsv[0] * 360 + 180) % 360) / 360;
				int[] tmp = IO.unpackRGB(Color.getHSBColor(hsv[0], hsv[1], hsv[2]).getRGB());
				color[1] = tmp[1];
				color[2] = tmp[2];
				color[3] = tmp[3];
			} else {
				for (int i = 1; i < color.length; i++) {
					color[i] = ~color[i] & 0xFF;
				}
			}

			out.setRGB(x, y, IO.packRGB(color));
		});

		return out;
	}

	public static BufferedImage grayscale(BufferedImage in) {
		BufferedImage source = IO.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		IO.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];
			int[] color = IO.unpackRGB(rgb);
			int luma = Calc.toLuma(color[1], color[2], color[3]);

			out.setRGB(x, y, IO.packRGB(color[0], luma, luma, luma));
		});

		return out;
	}
}
