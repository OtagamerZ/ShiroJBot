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
import com.kuuhaku.model.persistent.Account;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ShoukanDeck {
	private final Account acc;

	public ShoukanDeck(Account acc) {
		this.acc = acc;
	}

	public BufferedImage view(List<Champion> champs, List<Equipment> equips) throws IOException, InterruptedException {
		champs.sort(Comparator
				.comparing(Champion::getMana).reversed()
				.thenComparing(c -> c.getCard().getName(), String.CASE_INSENSITIVE_ORDER)
		);
		equips.sort(Comparator.comparing(e -> e.getCard().getName()));

		BufferedImage deck = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/deck.jpg")));

		Graphics2D g2d = deck.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 30));

		for (int i = 0, y = 0; i < champs.size(); i++, y = i / 6) {
			g2d.drawImage(champs.get(i).drawCard(acc, false), 76 + 279 * (i - 6 * y), 350 + 420 * y, null);
			Profile.printCenteredString(StringUtils.abbreviate(champs.get(i).getCard().getName(), 15), 225, 76 + 279 * (i - 6 * y), 740 + 420 * y, g2d);
		}

		for (int i = 0, y = 0; i < equips.size(); i++, y = i / 3) {
			g2d.drawImage(equips.get(i).drawCard(acc, false), 2022 + 279 * (i - 6 * y), 350 + 420 * y, null);
			Profile.printCenteredString(StringUtils.abbreviate(equips.get(i).getCard().getName(), 15), 225, 2022 + 279 * (i - 6 * y), 740 + 420 * y, g2d);
		}

		return deck;
	}
}
