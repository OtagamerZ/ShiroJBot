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

package com.kuuhaku.model.common;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.Helper;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ShoukanDeck {
	private final Account acc;
	private final Clan clan;
	final int SENSHI_COLUMNS = 6;
	final int EVOGEAR_COLUMNS = 4;

	public ShoukanDeck(Account acc, Clan clan) {
		this.acc = acc;
		this.clan = clan;
	}

	public ShoukanDeck(Account acc) {
		this.acc = acc;
		this.clan = null;
	}

	public BufferedImage view(Deck dk) throws IOException {
		List<Champion> champs = dk.getChampions();
		List<Equipment> equips = dk.getEquipments();
		List<Field> fields = dk.getFields();

		champs = champs.stream()
				.peek(c -> c.setAcc(acc))
				.collect(Collectors.toList());
		equips = equips.stream()
				.peek(e -> e.setAcc(acc))
				.flatMap(e -> ListUtils.union(List.of(e), Collections.nCopies(Math.max(e.getWeight(dk) - 1, 0), new Equipment())).stream())
				.collect(Collectors.toList());
		fields = fields.stream()
				.peek(f -> f.setAcc(acc))
				.collect(Collectors.toList());

		BufferedImage deck = Helper.getResourceAsImage(this.getClass(), "shoukan/deck.jpg");
		BufferedImage destiny = Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/destiny.png");

		assert deck != null;
		Graphics2D g2d = deck.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 30));

		g2d.drawImage(acc.getFrame().getBack(acc), 1746, 2241, null);

		for (int i = 0, y = 0; i < champs.size(); i++, y = i / SENSHI_COLUMNS) {
			Champion c = champs.get(i);
			g2d.drawImage(c.drawCard(false), 95 + 279 * (i - SENSHI_COLUMNS * y), 349 + 419 * y, null);
			if (dk.getDestinyDraw() != null && dk.getDestinyDraw().contains(i))
				g2d.drawImage(destiny, 85 + 279 * (i - SENSHI_COLUMNS * y), 339 + 419 * y, null);
			Profile.printCenteredString(StringUtils.abbreviate(c.getCard().getName(), 15), 225, 95 + 279 * (i - SENSHI_COLUMNS * y), 739 + 419 * y, g2d);
		}

		BufferedImage slotLock = Helper.getResourceAsImage(this.getClass(), "shoukan/slot_lock.png");
		for (int i = 0, y = 0; i < equips.size(); i++, y = i / EVOGEAR_COLUMNS) {
			Equipment e = equips.get(i);
			if (e.getTier() == 0)
				g2d.drawImage(slotLock, 2048 + 279 * (i - EVOGEAR_COLUMNS * y), 349 + 419 * y, null);
			else {
				g2d.drawImage(e.drawCard(false), 2048 + 279 * (i - EVOGEAR_COLUMNS * y), 349 + 419 * y, null);
				Profile.printCenteredString(StringUtils.abbreviate(e.getCard().getName(), 15), 225, 2048 + 279 * (i - EVOGEAR_COLUMNS * y), 739 + 419 * y, g2d);
			}
		}

		for (int i = 0; i < fields.size(); i++) {
			Field f = fields.get(i);
			g2d.drawImage(f.drawCard(false), 1769, 769 + (419 * i), null);
			Profile.printCenteredString(StringUtils.abbreviate(f.getCard().getName(), 15), 225, 1769, 1159 + (419 * i), g2d);
		}

		Pair<Race, Race> combo = dk.getCombo();
		if (combo.getLeft() != Race.NONE)
			g2d.drawImage(combo.getLeft().getIcon(), 1233, 122, 128, 128, null);
		if (combo.getRight() != Race.NONE)
			g2d.drawImage(combo.getRight().getIcon(), 1381, 136, 100, 100, null);

		g2d.dispose();
		return deck;
	}
}
