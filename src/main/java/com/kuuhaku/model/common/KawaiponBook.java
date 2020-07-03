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

import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.KawaiponRarity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class KawaiponBook {
	private final Set<KawaiponCard> cards;
	private static final Point[] slots = {
			new Point(180, 134),
			new Point(400, 134),
			new Point(620, 134),
			new Point(840, 134),
			new Point(1060, 134),
			new Point(1280, 134),

			new Point(180, 412),
			new Point(400, 412),
			new Point(620, 412),
			new Point(840, 412),
			new Point(1060, 412),
			new Point(1280, 412)
	};

	public KawaiponBook(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public List<BufferedImage> view() throws IOException {
		List<KawaiponCard> cards = new ArrayList<>(this.cards);
		cards.sort(Comparator
				.<KawaiponCard, KawaiponRarity>comparing(k -> k.getCard().getRarity(), Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
				.thenComparing(k -> k.getCard().getAnime(), Comparator.comparing(AnimeName::toString, String.CASE_INSENSITIVE_ORDER))
				.thenComparing(KawaiponCard::isFoil)
				.thenComparing(k -> k.getCard().getName(), String.CASE_INSENSITIVE_ORDER)
		);
		List<List<KawaiponCard>> chunks = new ArrayList<>();

		int pageCount = (int) Math.ceil(cards.size() / 12f);
		for (int i = 0; i < pageCount; i++) {
			ArrayList<KawaiponCard> chunk = new ArrayList<>();
			for (int p = 12 * i; p < cards.size() && p < 12 * (i + 1); p++) {
				chunk.add(cards.get(p));
			}
			chunks.add(chunk);
		}

		List<BufferedImage> pages = new ArrayList<>();
		final BufferedImage bg = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/background.jpg")));

		for (List<KawaiponCard> chunk : chunks) {
			BufferedImage back = new BufferedImage(bg.getWidth(), bg.getHeight(), bg.getType());
			Graphics2D g2d = back.createGraphics();
			g2d.setBackground(Color.black);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setFont(Profile.FONT.deriveFont(Font.BOLD, 23));

			g2d.drawImage(bg, 0, 0, null);
			for (int i = 0; i < chunk.size(); i++) {
				g2d.setBackground(Color.black);
				switch (chunk.get(i).getCard().getRarity()) {
					case COMMON:
						if (chunk.get(i).isFoil()) g2d.setColor(Color.decode("#FFFFFF").brighter());
						else g2d.setColor(Color.decode("#FFFFFF"));
						break;
					case UNCOMMON:
						if (chunk.get(i).isFoil()) g2d.setColor(Color.decode("#03BB85").brighter());
						else g2d.setColor(Color.decode("#03BB85"));
						break;
					case RARE:
						if (chunk.get(i).isFoil()) g2d.setColor(Color.decode("#70D1F4").brighter());
						else g2d.setColor(Color.decode("#70D1F4"));
						break;
					case ULTRA_RARE:
						if (chunk.get(i).isFoil()) g2d.setColor(Color.decode("#9966CC").brighter());
						else g2d.setColor(Color.decode("#9966CC"));
						break;
					case LEGENDARY:
						if (chunk.get(i).isFoil()) g2d.setColor(Color.decode("#DC9018").brighter());
						else g2d.setColor(Color.decode("#DC9018"));
						break;
					case ULTIMATE:
						g2d.setBackground(Color.decode("#FF006C"));
						g2d.setColor(Color.decode("#410066"));
						break;
				}
				g2d.drawImage(chunk.get(i).getCard().drawCard(chunk.get(i).isFoil()), slots[i].x, slots[i].y, 187, 280, null);
				if (slots[i].y == 134)
					Profile.printCenteredString(chunk.get(i).getName(), 187, slots[i].x, 105, g2d);
				else
					Profile.printCenteredString(chunk.get(i).getName(), 187, slots[i].x, 740, g2d);
			}

			g2d.dispose();

			pages.add(back);
		}

		return pages;
	}
}
