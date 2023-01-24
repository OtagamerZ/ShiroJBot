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

import java.awt.image.BufferedImage;

public abstract class ImageFilters {

	public static BufferedImage grayscale(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graph.forEachPixel(out, (x, y, rgb) -> {
			int luma = (int) (Calc.luminance(source.getRGB(x, y)) * 255);

			return Graph.packRGB((rgb >> 24) & 0xFF, luma, luma, luma);
		});

		return out;
	}

	public static BufferedImage silhouette(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graph.forEachPixel(out, (x, y, rgb) -> source.getRGB(x, y) & 0xFF000000);

		return out;
	}
}
