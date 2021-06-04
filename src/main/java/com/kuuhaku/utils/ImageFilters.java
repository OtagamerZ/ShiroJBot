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

package com.kuuhaku.utils;

import com.kuuhaku.model.enums.PixelOp;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageFilters {
	public static BufferedImage noise(BufferedImage in) {
		BufferedImage source = Helper.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Helper.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			out.setRGB(x, y, PixelOp.MULTIPLY.get(rgb, 0xFF000000 | (int) (Math.random() * 0xFFFFFF)));
		});

		return out;
	}

	public static BufferedImage shift(BufferedImage in, int offset) {
		BufferedImage source = Helper.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Helper.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			try {
				if (y % 2 == 0) out.setRGB(x - offset, y, rgb);
				else out.setRGB(x + offset, y, rgb);
			} catch (ArrayIndexOutOfBoundsException ignore) {
			}
		});

		return out;
	}

	public static BufferedImage mirror(BufferedImage in, int type) {
		BufferedImage source = Helper.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Helper.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			int pos = switch (type) {
				case 0 -> (int) ((out.getWidth() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getWidth()) * x)));
				case 1 -> out.getWidth() - (int) ((out.getWidth() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getWidth()) * x)));
				case 2 -> (int) ((out.getHeight() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getHeight()) * y)));
				case 3 -> out.getHeight() - (int) ((out.getHeight() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getHeight()) * y)));
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
		BufferedImage source = Helper.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage[] layers = {
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB)
		};

		int diag = Helper.hip(offset, offset);
		Helper.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			int[] colors = Helper.unpackRGB(source.getRGB(x, y));
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

		Helper.forEachPixel(out, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];

			int[] color = new int[3];
			for (int k = 0; k < layers.length; k++) {
				int c = layers[k].getRGB(x, y);
				color[k] = (c >> (16 - k * 8)) & 0xFF;
			}

			out.setRGB(x, y, 0xFF << 24 | color[0] << 16 | color[1] << 8 | color[2]);
		});

		return out;
	}

	public static BufferedImage invert(BufferedImage in, boolean onlyHue) {
		BufferedImage source = Helper.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Helper.forEachPixel(source, (coords, rgb) -> {
			int x = coords[0];
			int y = coords[1];
			int[] color = Helper.unpackRGB(rgb);

			if (onlyHue) {
				float[] hsv;
				hsv = Color.RGBtoHSB(color[1], color[2], color[3], null);
				hsv[0] = ((hsv[0] * 360 + 180) % 360) / 360;
				int[] tmp = Helper.unpackRGB(Color.getHSBColor(hsv[0], hsv[1], hsv[2]).getRGB());
				color[1] = tmp[1];
				color[2] = tmp[2];
				color[3] = tmp[3];
			} else {
				for (int i = 1; i < color.length; i++) {
					color[i] = ~color[i] & 0xFF;
				}
			}

			out.setRGB(x, y, color[0] << 24 | color[1] << 16 | color[2] << 8 | color[3]);
		});

		return out;
	}
}
