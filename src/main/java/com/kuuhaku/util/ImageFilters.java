/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.Constants;
import io.laniakia.algo.GlitchAlgorithm;
import io.laniakia.algo.GlitchController;
import io.laniakia.algo.PixelSlice;
import io.laniakia.filter.RGBShiftFilter;
import io.laniakia.util.GlitchTypes;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Map;

public abstract class ImageFilters {
	public static void grayscale(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> {
			int luma = (int) (Calc.luminance(rgb) * 255);

			in.setRGB(x, y, Graph.packRGB((rgb >> 24) & 0xFF, luma, luma, luma));
		});
	}

	public static void silhouette(BufferedImage in) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		Graph.forEachPixel(source, (x, y, rgb) -> in.setRGB(x, y, rgb & 0xFF000000));
	}

	public static void glitch(BufferedImage in, float severity) {
		try {
			BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_3BYTE_BGR);
			GlitchAlgorithm ga = new PixelSlice();
			ga.setPixelGlitchParameters(Map.of(
					"distortionLength", severity
			));

			byte[] buffer = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
			byte[] bytes = ga.glitchPixels(buffer);
			System.arraycopy(bytes, 0, buffer, 0, bytes.length);
			Graph.forEachPixel(source, (x, y, rgb) -> in.setRGB(x, y, rgb & 0xFF000000));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
