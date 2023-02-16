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
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum SlotSkin {
	DEFAULT,
//	AHEGAO,
	HEX("HOARDER_III"),
	PLANK("METANAUT"),
//	MISSING(""),
//	INVISIBLE(""),
	;

	private final String[] titles;

	SlotSkin() {
		this.titles = null;
	}

	SlotSkin(String... titles) {
		this.titles = titles;
	}

	public BufferedImage getImage(Side side, boolean legacy) {
		String s = side.name().toLowerCase();

		BufferedImage bi = IO.getResourceAsImage("shoukan/side/" + name().toLowerCase() + "_" + s + ".webp");
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		Graph.applyMask(bi, IO.getResourceAsImage("shoukan/mask/slot_" + s + (legacy ? "_legacy" : "") + "_mask.webp"), 0);
		g2d.drawImage(IO.getResourceAsImage("shoukan/overlay/" + s + (legacy ? "_legacy" : "") + ".webp"), -5, -5, null);

		g2d.dispose();

		return bi;
	}

	public String getName(I18N locale) {
		return locale.get("skin/" + name());
	}

	public String getDescription(I18N locale) {
		return locale.get("skin/" + name() + "_desc");
	}

	public java.util.List<Title> getTitles() {
		if (titles == null) return List.of();

		List<Title> out = new ArrayList<>();
		for (String title : titles) {
			out.add(DAO.find(Title.class, title));
		}

		return out;
	}

	public boolean canUse(Account acc) {
		if (titles == null) return true;

		for (String title : titles) {
			if (!acc.hasTitle(title)) return false;
		}

		return true;
	}

	public static SlotSkin getByName(String name) {
		return Arrays.stream(values()).filter(fc -> Utils.equalsAny(name, fc.name(), fc.toString())).findFirst().orElse(null);
	}
}
