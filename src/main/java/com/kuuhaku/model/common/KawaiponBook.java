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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.RarityColors;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.NContract;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class KawaiponBook {
	private final Set<KawaiponCard> cards;
	private static final int COLUMN_COUNT = 10;

	public KawaiponBook(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public KawaiponBook() {
		this.cards = null;
	}

	public BufferedImage view(List<Card> cardList, String title, boolean foil) throws IOException, InterruptedException {
		String text;
		if (foil) text = "« " + title + " »";
		else text = title;
		assert this.cards != null;
		List<KawaiponCard> cards = new ArrayList<>(this.cards);
		cardList.sort(Comparator
				.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
				.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER)
		);
		List<List<KawaiponCard>> chunks = Helper.chunkify(cardList.stream().map(c -> new KawaiponCard(c, foil)).collect(Collectors.toList()), COLUMN_COUNT);
		chunks.removeIf(List::isEmpty);

		BufferedImage header = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/header.png")));
		BufferedImage footer = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/footer.png")));
		BufferedImage slot = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/slot.png")));

		Graphics2D g2d = header.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.BOLD, 72));
		if (foil) g2d.setColor(Color.yellow);
		Profile.printCenteredString(text, 1904, 36, 168, g2d);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			System.gc();
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (299 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g.drawImage(header, 0, 0, null);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 299 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		AtomicReference<BufferedImage> result = new AtomicReference<>();
		ExecutorService th = Executors.newFixedThreadPool(10);
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				try {
					BufferedImage row = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/row.png")));
					Graphics2D g = row.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					for (int i = 0; i < chunks.get(finalC).size(); i++) {
						if (cards.contains(chunks.get(finalC).get(i))) {
							g.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));
							RarityColors rc = RarityColorsDAO.getColor(chunks.get(finalC).get(i).getCard().getRarity());

							g.setBackground(rc.getSecondary());
							if (foil) g.setColor(rc.getPrimary().brighter());
							else g.setColor(rc.getPrimary());

							g.drawImage(chunks.get(finalC).get(i).getCard().drawCard(foil), 54 + 198 * i, 24, 160, 250, null);
							Profile.printCenteredString(StringUtils.abbreviate(chunks.get(finalC).get(i).getName(), 15), 160, 54 + 198 * i, 298, g);
						} else if (chunks.get(finalC).get(i).getCard().getRarity().equals(KawaiponRarity.ULTIMATE)) {
							g.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));
							g.setBackground(Color.black);
							g.setColor(Color.white);

							g.drawImage(slot, 54 + 198 * i, 24, 160, 250, null);
							Profile.printCenteredString(StringUtils.abbreviate(chunks.get(finalC).get(i).getName(), 15), 160, 54 + 198 * i, 298, g);
						} else {
							g.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));
							g.setBackground(Color.black);
							g.setColor(Color.white);

							g.drawImage(slot, 54 + 198 * i, 24, 160, 250, null);
							Profile.printCenteredString("???", 160, 54 + 198 * i, 298, g);
						}
					}

					g.dispose();

					result.set(act.addSignature(finalC, row));
					row.flush();
				} catch (IOException ignore) {
				}
			});
		}

		while (result.get() == null) {
			Thread.sleep(250);
		}

		return result.get();
	}

	public BufferedImage view(List<Drawable> cardList, Account acc, String title, boolean senshi) throws IOException, InterruptedException {
		if (senshi)
			cardList.sort(Comparator
					.comparing(d -> ((Champion) d).getMana())
					.reversed()
					.thenComparing(c -> ((Champion) c).getCard().getName(), String.CASE_INSENSITIVE_ORDER)
			);
		else
			cardList.sort(Comparator
					.comparing(d -> ((Equipment) d).getTier())
					.reversed()
					.thenComparing(c -> ((Equipment) c).getCard().getName(), String.CASE_INSENSITIVE_ORDER)
			);

		List<List<Drawable>> chunks = Helper.chunkify(cardList, COLUMN_COUNT);
		chunks.removeIf(List::isEmpty);

		BufferedImage header = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/header.png")));
		BufferedImage footer = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/footer.png")));

		Graphics2D g2d = header.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(Profile.FONT.deriveFont(Font.BOLD, 72));
		Profile.printCenteredString(title, 1904, 36, 168, g2d);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			System.gc();
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (299 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g.drawImage(header, 0, 0, null);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 299 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		AtomicReference<BufferedImage> result = new AtomicReference<>();
		ExecutorService th = Executors.newFixedThreadPool(10);
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				try {
					BufferedImage row = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/row.png")));
					Graphics2D g = row.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(Color.white);

					for (int i = 0; i < chunks.get(finalC).size(); i++) {
						g.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

						g.drawImage(chunks.get(finalC).get(i).drawCard(false), 54 + 198 * i, 24, 160, 250, null);
						Profile.printCenteredString(StringUtils.abbreviate(chunks.get(finalC).get(i).getCard().getName(), 15), 160, 54 + 198 * i, 298, g);
					}

					g.dispose();

					result.set(act.addSignature(finalC, row));
					row.flush();
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
