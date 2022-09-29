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

package com.kuuhaku.model.enums.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

public enum SlotSkin {
	DEFAULT, AHEGAO, HEX;

	public BufferedImage getImage(Side side, boolean legacy) {
		String s = side.name().toLowerCase();

		BufferedImage bi = IO.getResourceAsImage("shoukan/side/" + name().toLowerCase() + "_" + s + ".webp");
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		Graph.applyMask(bi, IO.getResourceAsImage("shoukan/mask/slot_" + s + (legacy ? "_legacy" : "") + "_mask.webp"), 0, true);
		g2d.drawImage(IO.getResourceAsImage("shoukan/overlay/" + s + (legacy ? "_legacy" : "") + ".webp"), -5, -5, null);

		g2d.dispose();

		return bi;
	}
}
