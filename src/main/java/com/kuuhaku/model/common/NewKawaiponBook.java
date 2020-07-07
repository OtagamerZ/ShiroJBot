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

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.RarityColorsDAO;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.RarityColors;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.KawaiponRarity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class NewKawaiponBook {
	private final Set<KawaiponCard> cards;

	public NewKawaiponBook(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public BufferedImage view(AnimeName anime) throws IOException {
		int totalCards = CardDAO.getCardsByAnime(anime).size();
		List<KawaiponCard> cards = new ArrayList<>(this.cards);
		cards.sort(Comparator
				.<KawaiponCard, KawaiponRarity>comparing(k -> k.getCard().getRarity(), Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
				.thenComparing(k -> k.getCard().getName(), String.CASE_INSENSITIVE_ORDER)
		);
		List<List<KawaiponCard>> chunks = new ArrayList<>();

		int rowCount = (int) Math.ceil(totalCards / 5f);
		for (int i = 0; i < rowCount; i++) {
			ArrayList<KawaiponCard> chunk = new ArrayList<>();
			for (int p = 5 * i; p < totalCards && p < 5 * (i + 1); p++) {
				if (p < cards.size()) chunk.add(cards.get(p));
				else chunk.add(null);
			}
			chunks.add(chunk);
		}

		List<BufferedImage> rows = new ArrayList<>();
		final BufferedImage header = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/header.jpg")));
		final BufferedImage footer = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/footer.jpg")));
		final BufferedImage slot = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/slot.png")));

		Graphics2D g2d = header.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.BOLD, 210));
		Profile.printCenteredString(anime.toString(), 2100, 75, 417, g2d);

		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 42));

		//2100 x 417 (75)
		for (List<KawaiponCard> chunk : chunks) {
			BufferedImage row = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/row.jpg")));
			g2d = row.createGraphics();

			for (int i = 0; i < chunk.size(); i++) {
				if (chunk.get(i) != null) {
					RarityColors rc = RarityColorsDAO.getColor(chunk.get(i).getCard().getRarity());

					g2d.setBackground(rc.getSecondary());
					if (chunk.get(i).isFoil()) g2d.setColor(rc.getPrimary().brighter());
					else g2d.setColor(rc.getPrimary());

					g2d.drawImage(chunk.get(i).getCard().drawCard(chunk.get(i).isFoil()), 117 + 420 * i, 65, 338, 526, null);
					Profile.printCenteredString(chunk.get(i).getName(), 338, 117 + 420 * i, 635, g2d);
				} else {
					g2d.setBackground(Color.black);
					g2d.setColor(Color.white);

					g2d.drawImage(slot, 117 + 420 * i, 65, 338, 526, null);
					Profile.printCenteredString("???", 338, 117 + 420 * i, 635, g2d);
				}
			}

			g2d.dispose();

			rows.add(row);
		}

		BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (656 * rows.size()), BufferedImage.TYPE_INT_ARGB);
		g2d = bg.createGraphics();

		g2d.drawImage(header, 0, 0, null);

		for (int i = 0; i < rows.size(); i++) {
			g2d.drawImage(rows.get(i), 0, header.getHeight() + 656 * i, null);
		}

		g2d.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
		g2d.dispose();

		return bg;
	}
}
