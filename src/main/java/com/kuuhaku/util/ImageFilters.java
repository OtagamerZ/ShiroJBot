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

package com.kuuhaku.util;

import com.kuuhaku.model.enums.PixelOp;

import java.awt.image.BufferedImage;

public abstract class ImageFilters {
	public static BufferedImage noise(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> out.setRGB(x, y, PixelOp.MULTIPLY.get(rgb, 0xFF000000 | Calc.rng(0, 0xFFFFFF))));

		return out;
	}

	public static BufferedImage shift(BufferedImage in, int type) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> {
			try {
				if ((type == 0 ? y : type == 1 ? x : y + x) % 2 == 0) out.setRGB(x, y, 0);
				else out.setRGB(x, y, rgb);
			} catch (ArrayIndexOutOfBoundsException ignore) {
			}
		});

		return out;
	}

	public static BufferedImage mirror(BufferedImage in, int type) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> {
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
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage[] layers = {
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB)
		};

		int diag = (int) Math.hypot(offset, offset);
		Graph.forEachPixel(source, (x, y, rgb) -> {
			int[] colors = Graph.unpackRGB(source.getRGB(x, y));
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

		Graph.forEachPixel(out, (x, y, rgb) -> {
			int[] color = new int[3];
			for (int k = 0; k < layers.length; k++) {
				int c = layers[k].getRGB(x, y);
				color[k] = (c >> (16 - k * 8)) & 0xFF;
			}

			out.setRGB(x, y, Graph.packRGB(0xFF, color[0], color[1], color[2]));
		});

		return out;
	}

	public static BufferedImage invert(BufferedImage in, boolean onlyHue) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> {
			int[] color;

			if (onlyHue) {
				color = Graph.unpackRGB(Graph.rotate(rgb, 180));
			} else {
				color = Graph.unpackRGB(rgb);
				for (int i = 1; i < color.length; i++) {
					color[i] = ~color[i] & 0xFF;
				}
			}

			out.setRGB(x, y, Graph.packRGB(color));
		});

		return out;
	}

	public static BufferedImage grayscale(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> {
			int luma = (int) (Calc.luminance(rgb) * 255);

			out.setRGB(x, y, Graph.packRGB((rgb >> 24) & 0xFF, luma, luma, luma));
		});

		return out;
	}

	public static BufferedImage silhouette(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> {
			int[] color = Graph.unpackRGB(rgb);

			out.setRGB(x, y, Graph.packRGB(color[0], 0, 0, 0));
		});

		return out;
	}
}
