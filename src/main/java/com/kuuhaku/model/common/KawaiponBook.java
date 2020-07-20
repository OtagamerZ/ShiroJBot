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

import com.kuuhaku.controller.postgresql.RarityColorsDAO;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.RarityColors;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.KawaiponRarity;
import com.kuuhaku.utils.NContract;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class KawaiponBook {
	private final Set<KawaiponCard> cards;

	public KawaiponBook(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public BufferedImage view(List<Card> cardList, String title, boolean foil) throws IOException, InterruptedException {
		int totalCards = cardList.size();
		String text;
		if (foil) text = "« " + title + " »";
		else text = title;
		List<KawaiponCard> cards = new ArrayList<>(this.cards);
		cardList.sort(Comparator
				.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
				.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER)
		);
		List<List<KawaiponCard>> chunks = new ArrayList<>();

		int rowCount = (int) Math.ceil(totalCards / 5f);
		for (int i = 0; i < rowCount; i++) {
			ArrayList<KawaiponCard> chunk = new ArrayList<>();
			for (int p = 5 * i; p < totalCards && p < 5 * (i + 1); p++) {
				chunk.add(new KawaiponCard(cardList.get(p), foil));
			}
			chunks.add(chunk);
		}

		final BufferedImage header = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/header.jpg")));
		final BufferedImage footer = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/footer.jpg")));
		final BufferedImage slot = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/slot.png")));

		Graphics2D g2d = header.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.BOLD, Helper.clamp(12 * 210 / text.length(), 105, 210)));
		if (foil) g2d.setColor(Color.yellow);
		Profile.printCenteredString(text, 2100, 75, 400, g2d);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (656 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g.drawImage(header, 0, 0, null);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 656 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		AtomicReference<BufferedImage> result = new AtomicReference<>();
		ExecutorService th = Executors.newCachedThreadPool();
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				try {
					BufferedImage row = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/row.jpg")));
					Graphics2D g = row.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					
					for (int i = 0; i < chunks.get(finalC).size(); i++) {
						if (cards.contains(chunks.get(finalC).get(i))) {
							g.setFont(Profile.FONT.deriveFont(Font.PLAIN, Helper.clamp(20 * 42 / chunks.get(finalC).get(i).getName().length(), 38, 42)));
							RarityColors rc = RarityColorsDAO.getColor(chunks.get(finalC).get(i).getCard().getRarity());

							g.setBackground(rc.getSecondary());
							if (foil) g.setColor(rc.getPrimary().brighter());
							else g.setColor(rc.getPrimary());

							g.drawImage(chunks.get(finalC).get(i).getCard().drawCard(foil), 117 + 420 * i, 65, 338, 526, null);
							Profile.printCenteredString(chunks.get(finalC).get(i).getName(), 338, 117 + 420 * i, 635, g);
						} else if (chunks.get(finalC).get(i).getCard().getRarity().equals(KawaiponRarity.ULTIMATE)) {
							g.setFont(Profile.FONT.deriveFont(Font.PLAIN, 42));
							g.setBackground(Color.black);
							g.setColor(Color.white);

							g.drawImage(slot, 117 + 420 * i, 65, 338, 526, null);
							Profile.printCenteredString(chunks.get(finalC).get(i).getName(), 338, 117 + 420 * i, 635, g);
						} else {
							g.setFont(Profile.FONT.deriveFont(Font.PLAIN, 42));
							g.setBackground(Color.black);
							g.setColor(Color.white);

							g.drawImage(slot, 117 + 420 * i, 65, 338, 526, null);
							Profile.printCenteredString("???", 338, 117 + 420 * i, 635, g);
						}
					}

					g.dispose();

					result.set(act.addSignature(finalC, row));
				} catch (IOException ignore) {
				}
			});
		}

		while (result.get() == null) {
			Thread.sleep(250);
		}

		return result.get();
	}
}
