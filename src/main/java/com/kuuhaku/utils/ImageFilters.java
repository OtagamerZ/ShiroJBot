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

import java.awt.image.BufferedImage;

public class ImageFilters {
	public static BufferedImage noise(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int i = 0;
		while (true) {
			int x = i % out.getWidth();
			int y = i / out.getWidth();

			if (x >= out.getWidth() || y >= out.getHeight()) break;
			int color = in.getRGB(x, y);

			out.setRGB(x, y, PixelOp.MULTIPLY.get(color, 0xFF000000 | (int) (Math.random() * 0xFFFFFF)));
			i++;
		}

		return out;
	}

	public static BufferedImage shift(BufferedImage in, int offset) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int i = 0;
		while (true) {
			int x = i % out.getWidth();
			int y = i / out.getWidth();

			if (x >= out.getWidth() || y >= out.getHeight()) break;

			try {
				if (y % 2 == 0) out.setRGB(x - offset, y, in.getRGB(x, y));
				else out.setRGB(x + offset, y, in.getRGB(x, y));
			} catch (ArrayIndexOutOfBoundsException ignore) {
			}
			i++;
		}

		return out;
	}

	public static BufferedImage mirror(BufferedImage in, int type) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int i = 0;
		while (true) {
			int x = i % out.getWidth();
			int y = i / out.getWidth();

			if (x >= out.getWidth() || y >= out.getHeight()) break;

			int pos = switch (type) {
				case 0 -> (int) ((out.getWidth() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getWidth()) * x)));
				case 1 -> out.getWidth() - (int) ((out.getWidth() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getWidth()) * x)));
				case 2 -> (int) ((out.getHeight() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getHeight()) * y)));
				case 3 -> out.getHeight() - (int) ((out.getHeight() / (2d * Math.PI)) * Math.acos(Math.cos(((2 * Math.PI) / out.getHeight()) * y)));
				default -> throw new IllegalStateException("Unexpected value: " + type);
			};

			try {
				out.setRGB(x, y, type > 1 ? in.getRGB(x, pos) : in.getRGB(pos, y));
			} catch (ArrayIndexOutOfBoundsException ignore) {
			}

			i++;
		}

		return out;
	}

	public static BufferedImage glitch(BufferedImage in, int offset) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage[] layers = {
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB),
				new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_ARGB)
		};

		int diag = Helper.hip(offset, offset);
		int i = 0;
		while (true) {
			int x = i % out.getWidth();
			int y = i / out.getWidth();

			if (x >= out.getWidth() || y >= out.getHeight()) break;
			int[] rgb = Helper.unpackRGB(in.getRGB(x, y));
			int[] ext = new int[3];

			ext[0] = (rgb[1] / 3) << 24 | rgb[1] << 16;
			ext[1] = (rgb[2] / 3) << 24 | rgb[2] << 8;
			ext[2] = (rgb[3] / 3) << 24 | rgb[3];

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

			i++;
		}

		i = 0;
		while (true) {
			int x = i % out.getWidth();
			int y = i / out.getWidth();

			if (x >= out.getWidth() || y >= out.getHeight()) break;

			int[] color = new int[3];
			for (int k = 0; k < layers.length; k++) {
				color[k] = (layers[k].getRGB(x, y) >> (16 - k * 8)) & 0xFF;
			}

			out.setRGB(x, y, 0xFF << 24 | color[0] << 16 | color[1] << 8 | color[2]);

			i++;
		}

		return out;
	}
}
