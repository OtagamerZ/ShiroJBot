/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import net.coobird.thumbnailator.filters.ImageFilter;

import java.awt.image.BufferedImage;

public class BlurFilter implements ImageFilter {
	private final int radius;

	public BlurFilter(int radius) {
		this.radius = radius;
	}

	@Override
	public BufferedImage apply(BufferedImage img) {
		StackBlur sb = new StackBlur();
		sb.blur(img, radius);

		return img;
	}
}
