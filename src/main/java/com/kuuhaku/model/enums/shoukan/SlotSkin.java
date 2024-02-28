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

package com.kuuhaku.model.enums.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.Currency;
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
//	AHEGAO(""),
	HEX("HOARDER_III"),
	PLANK(3500, Currency.CR, "METANAUT"),
	MISSING(10000, Currency.CR),
	INVISIBLE("DEUS_VULT", "REBELLION", "HOARDER_II"),
	LEGACY("VETERAN"),
	NEBULA(5, Currency.GEM, "METANAUT", "TALKER_III", "UNTOUCHABLE", "MEDIC"),
	GRAFITTI(5000, Currency.CR, "REBELLION", "SURVIVOR", "RUTHLESS"),
	RAINBOW(1, Currency.GEM),
	DIGITAL(5000, Currency.CR, "TALKER_II"),
	SYNTHWAVE("PARADOX", "FROM_ABYSS", "MONKE", "ONE_GOD"),
	;

	private final int price;
	private final Currency currency;
	private final String[] titles;

	SlotSkin() {
		this.price = 0;
		this.currency = null;
		this.titles = null;
	}

	SlotSkin(String... titles) {
		this.price = 0;
		this.currency = null;
		this.titles = titles;
	}

	SlotSkin(int price, Currency currency, String... titles) {
		this.price = price;
		this.currency = currency;
		this.titles = titles;
	}

	public BufferedImage getImage(Side side, boolean legacy) {
		String s = side.name().toLowerCase();
		BufferedImage overlay = IO.getResourceAsImage("shoukan/overlay/" + s + (legacy ? "_legacy" : "") + ".png");
		if (this == INVISIBLE) return overlay;

		BufferedImage bi = new BufferedImage(overlay.getWidth(), overlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		BufferedImage theme = IO.getResourceAsImage("shoukan/side/" + name().toLowerCase() + "_" + s + ".png");
		Graph.applyMask(theme, IO.getResourceAsImage("shoukan/mask/slot_" + s + (legacy ? "_legacy" : "") + "_mask.png"), 0);

		g2d.drawImage(theme, 5, 5, null);
		g2d.drawImage(overlay, 0, 0, null);

		g2d.dispose();

		return bi;
	}

	public String getName(I18N locale) {
		return locale.get("skin/" + name());
	}

	public String getDescription(I18N locale) {
		return locale.get("skin/" + name() + "_desc");
	}

	public List<Title> getTitles() {
		if (titles == null) return List.of();

		List<Title> out = new ArrayList<>();
		for (String title : titles) {
			out.add(DAO.find(Title.class, title));
		}

		return out;
	}

	public int getPrice() {
		return price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public boolean canUse(Account acc) {
		if (titles == null) return true;

		for (String title : titles) {
			if (!acc.hasTitle(title)) return false;
		}

		if (price > 0) {
			return !acc.getDynValue("ss_" + name().toLowerCase()).isBlank();
		}

		return true;
	}

	public static SlotSkin getByName(String name) {
		return Arrays.stream(values()).parallel()
				.filter(fc -> Utils.equalsAny(name, fc.name(), fc.toString()))
				.findAny().orElse(null);
	}
}
