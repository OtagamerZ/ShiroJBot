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

import com.kuuhaku.utils.KawaiponCard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class KawaiponBook {
	private final Set<KawaiponCard> cards;
	private static final Point[] slots = {
			new Point(190, 144),
			new Point(410, 144),
			new Point(630, 144),
			new Point(850, 144),
			new Point(1070, 144),
			new Point(1290, 144),

			new Point(190, 422),
			new Point(410, 422),
			new Point(630, 422),
			new Point(850, 422),
			new Point(1070, 422),
			new Point(1290, 422)
	};

	public KawaiponBook(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public List<BufferedImage> view() throws IOException {
		List<KawaiponCard> cards = new ArrayList<>(this.cards);
		cards.sort(Comparator.comparingInt(k -> k.getRarity().getIndex()));
		List<List<KawaiponCard>> chunks = new ArrayList<>();

		int pageCount = (int) Math.ceil(cards.size() / 12f);
		for (int i = 0; i < pageCount; i++) {
			ArrayList<KawaiponCard> chunk = new ArrayList<>();
			for (int p = 12 * i; p < cards.size(); p++) {
				chunk.add(cards.get(p));
			}
			chunks.add(chunk);
		}

		List<BufferedImage> pages = new ArrayList<>();
		final BufferedImage bg = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/background.jpg")));

		for (List<KawaiponCard> chunk : chunks) {
			BufferedImage back = bg.getSubimage(0, 0, bg.getWidth(), bg.getHeight());
			Graphics2D g2d = back.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			for (int i = 0; i < chunk.size(); i++) {
				g2d.drawImage(chunk.get(i).getCard(), slots[i].x, slots[i].y, null);
			}

			g2d.dispose();

			pages.add(back);
		}

		return pages;
	}
}
