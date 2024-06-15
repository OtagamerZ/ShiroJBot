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
import io.laniakia.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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

	// Copied from io.laniakia.algo.PixelSlice
	public static void glitch(BufferedImage in, float distortion) {
		BufferedImage source = Graph.toColorSpace(in, BufferedImage.TYPE_INT_ARGB);
		int[] pixelCanvasArray = ImageUtil.getCanvasFormatPixels(source);
		int randomSliceAmount = -1;
		for (int i = 0; i < source.getWidth(); i++) {
			if (Math.random() > 0.95) {
				randomSliceAmount = (int) Math.floor(((1.0 - distortion) * ThreadLocalRandom.current().nextFloat() + distortion) * source.getHeight());
			}

			if (Math.random() > 0.95) {
				randomSliceAmount = 0;
			}

			for (int j = 0; j < source.getHeight(); j++) {
				int pixelCanvasPosition = (i + j * source.getWidth()) * 4;
				int rowDistortionOffsetStart = j + randomSliceAmount;
				if (rowDistortionOffsetStart > source.getHeight() - 1) {
					rowDistortionOffsetStart = rowDistortionOffsetStart - source.getHeight();
				}

				int rowDistortionOffsetEnd = (i + rowDistortionOffsetStart * source.getWidth()) * 4;
				for (int k = 0; k < 4; k++) {
					if ((rowDistortionOffsetEnd + k) < 0 || (rowDistortionOffsetEnd + k) > pixelCanvasArray.length) {
						continue;
					}
					pixelCanvasArray[rowDistortionOffsetEnd + k] = pixelCanvasArray[pixelCanvasPosition + k];
				}
			}
		}

		Graph.forEachPixel(ImageUtil.getImageFromCanvasPixelArray(pixelCanvasArray, source), in::setRGB);
	}
}
