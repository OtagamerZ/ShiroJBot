/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ImageFilters;
import com.kuuhaku.utils.NContract;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class KawaiponBook {
	private static final int COLUMN_COUNT = 20;
	private static final int CARD_WIDTH = 160;
	private static final int CARD_HEIGHT = 250;
	private final BufferedImage header;
	private final BufferedImage row;
	private final BufferedImage footer;
	private final BufferedImage slot;

	public KawaiponBook() {
		header = Helper.getResourceAsImage(this.getClass(), "kawaipon/header.png");
		row = Helper.getResourceAsImage(this.getClass(), "kawaipon/row.png");
		footer = Helper.getResourceAsImage(this.getClass(), "kawaipon/footer.png");
		slot = Helper.getResourceAsImage(this.getClass(), "kawaipon/slot.png");

		assert Helper.notNull(header, row, footer, slot);
	}

	public BufferedImage view(String uid, String name) throws InterruptedException {
		List<Card> cards = CardDAO.getValidAnimeNames().stream()
				.sorted(Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER))
				.map(CardDAO::getUltimate)
				.collect(Collectors.toList());
		List<List<Card>> chunks = Helper.chunkify(cards, COLUMN_COUNT);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (299 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g.drawImage(header, 0, 0, null);
			g.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 72));
			Profile.printCenteredString("Coleções de " + name, 4026, 35, 168, g);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 299 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		BufferedImage frame = Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/new/ultimate.png");
		ExecutorService th = Executors.newFixedThreadPool(5);
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				BufferedImage bi = new BufferedImage(row.getWidth(), row.getHeight(), row.getType());
				Graphics2D g = bi.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.drawImage(row, 0, 0, null);

				List<Card> chunk = chunks.get(finalC);
				for (int i = 0; i < chunk.size(); i++) {
					Card kc = chunk.get(i);
					BufferedImage card = kc.drawUltimate(uid);

					int width = 4026 / COLUMN_COUNT;
					int actualWidth = width * chunk.size();
					int x = 35 + ((4026 - actualWidth) / 2) + ((width - CARD_WIDTH) / 2) + width * i;

					int height = row.getHeight();
					int y = ((height - CARD_HEIGHT) / 2);

					g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
					g.setBackground(Color.black);
					g.setColor(Color.white);
					g.drawImage(slot, x, y, CARD_WIDTH, CARD_HEIGHT, null);

					g.drawImage(card, x, y, CARD_WIDTH, CARD_HEIGHT, null);
					Profile.printCenteredString(StringUtils.abbreviate(kc.getName(), 15), CARD_WIDTH, x, y + 274, g);
				}

				g.dispose();

				act.addSignature(finalC, bi);
			});
		}

		try {
			return act.get();
		} catch (ExecutionException e) {
			return null;
		} finally {
			th.shutdownNow();
		}
	}

	public BufferedImage view(String uid, AddedAnime anime, boolean foil) throws InterruptedException {
		Map<String, Boolean> col = CardDAO.getCardsByAnime(uid, anime == null ? "" : anime.getName(), foil);

		List<KawaiponCard> cards;
		if (anime == null)
			cards = CardDAO.getAllCards().stream()
					.sorted(Comparator
							.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
							.thenComparing(c -> c.getAnime().getName(), String.CASE_INSENSITIVE_ORDER)
							.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER))
					.map(c -> new KawaiponCard(c, false))
					.collect(Collectors.toList());
		else
			cards = CardDAO.getCardsByAnime(anime.getName()).stream()
					.sorted(Comparator
							.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
							.thenComparing(c -> c.getAnime().getName(), String.CASE_INSENSITIVE_ORDER)
							.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER))
					.map(c -> new KawaiponCard(c, false))
					.collect(Collectors.toList());
		List<List<KawaiponCard>> chunks = Helper.chunkify(cards, COLUMN_COUNT);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (299 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			String title;
			if (foil)
				if (anime == null)
					title = "« Todas as cartas »";
				else
					title = "« " + anime + " »";
			else {
				if (anime == null)
					title = "Todas as cartas";
				else
					title = anime.toString();
			}

			g.drawImage(header, 0, 0, null);
			g.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 72));
			if (foil) g.setColor(Color.orange);
			Profile.printCenteredString(title, 4026, 35, 168, g);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 299 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		ExecutorService th = Executors.newFixedThreadPool(5);
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				BufferedImage bi = new BufferedImage(row.getWidth(), row.getHeight(), row.getType());
				Graphics2D g = bi.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.drawImage(row, 0, 0, null);

				List<KawaiponCard> chunk = chunks.get(finalC);
				for (int i = 0; i < chunk.size(); i++) {
					KawaiponCard kc = chunk.get(i);
					BufferedImage card = kc.getCard().drawCard(foil);

					int width = 4026 / COLUMN_COUNT;
					int actualWidth = width * chunk.size();
					int x = 35 + ((4026 - actualWidth) / 2) + ((width - CARD_WIDTH) / 2) + width * i;

					int height = row.getHeight();
					int y = ((height - CARD_HEIGHT) / 2);

					g.setBackground(Color.black);
					if (col.get(kc.getCard().getId())) {
						g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
						RarityColors rc = RarityColorsDAO.getColor(kc.getCard().getRarity());
						g.setColor(foil ? rc.getSecondary() : rc.getPrimary());

						g.drawImage(card, x, y, CARD_WIDTH, CARD_HEIGHT, null);
						Profile.printCenteredString(StringUtils.abbreviate(kc.getName(), 15), CARD_WIDTH, x, y + 274, g);
					} else {
						g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
						g.setColor(Color.white);

						g.drawImage(slot, x, y, CARD_WIDTH, CARD_HEIGHT, null);
						Profile.printCenteredString("???", CARD_WIDTH, x, y + 274, g);
					}
				}

				g.dispose();

				act.addSignature(finalC, bi);
			});
		}

		try {
			return act.get();
		} catch (ExecutionException e) {
			return null;
		} finally {
			th.shutdownNow();
		}
	}

	public BufferedImage view(String uid, KawaiponRarity rarity, boolean foil) throws InterruptedException {
		Map<String, Boolean> col = CardDAO.getCardsByRarity(uid, rarity == null ? "" : rarity.name(), foil);

		List<KawaiponCard> cards;
		if (rarity == null)
			cards = CardDAO.getAllCards().stream()
					.sorted(Comparator
							.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
							.thenComparing(c -> c.getAnime().getName(), String.CASE_INSENSITIVE_ORDER)
							.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER))
					.map(c -> new KawaiponCard(c, false))
					.collect(Collectors.toList());
		else
			cards = CardDAO.getCardsByRarity(rarity).stream()
					.sorted(Comparator
							.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
							.thenComparing(c -> c.getAnime().getName(), String.CASE_INSENSITIVE_ORDER)
							.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER))
					.map(c -> new KawaiponCard(c, false))
					.collect(Collectors.toList());
		List<List<KawaiponCard>> chunks = Helper.chunkify(cards, COLUMN_COUNT);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (299 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			String title;
			if (foil)
				if (rarity == null)
					title = "« Todas as cartas »";
				else
					title = "« " + rarity + " »";
			else {
				if (rarity == null)
					title = "Todas as cartas";
				else
					title = rarity.toString();
			}

			g.drawImage(header, 0, 0, null);
			g.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 72));
			if (foil) g.setColor(Color.orange);
			Profile.printCenteredString(title, 4026, 35, 168, g);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 299 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		ExecutorService th = Executors.newFixedThreadPool(5);
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				BufferedImage bi = new BufferedImage(row.getWidth(), row.getHeight(), row.getType());
				Graphics2D g = bi.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.drawImage(row, 0, 0, null);

				List<KawaiponCard> chunk = chunks.get(finalC);
				for (int i = 0; i < chunk.size(); i++) {
					KawaiponCard kc = chunk.get(i);
					BufferedImage card = kc.getCard().drawCard(foil);

					int width = 4026 / COLUMN_COUNT;
					int actualWidth = width * chunk.size();
					int x = 35 + ((4026 - actualWidth) / 2) + ((width - CARD_WIDTH) / 2) + width * i;

					int height = row.getHeight();
					int y = ((height - CARD_HEIGHT) / 2);

					g.setBackground(Color.black);
					if (col.get(kc.getCard().getId())) {
						g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
						RarityColors rc = RarityColorsDAO.getColor(kc.getCard().getRarity());
						g.setColor(foil ? rc.getSecondary() : rc.getPrimary());

						g.drawImage(card, x, y, CARD_WIDTH, CARD_HEIGHT, null);
						Profile.printCenteredString(StringUtils.abbreviate(kc.getName(), 15), CARD_WIDTH, x, y + 274, g);
					} else {
						g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
						g.setColor(Color.white);

						g.drawImage(slot, x, y, CARD_WIDTH, CARD_HEIGHT, null);
						Profile.printCenteredString("???", CARD_WIDTH, x, y + 274, g);
					}
				}

				g.dispose();

				act.addSignature(finalC, bi);
			});
		}

		try {
			return act.get();
		} catch (ExecutionException e) {
			return null;
		} finally {
			th.shutdownNow();
		}
	}

	public BufferedImage view(List<Drawable> cardList, Account acc, String title) throws InterruptedException {
		AtomicBoolean showAvailable = new AtomicBoolean(true);
		List<Drawable> cards = cardList.stream()
				.peek(d -> d.setAcc(acc))
				.sorted(Comparator
						.<Drawable>comparingInt(d -> {
							if (d instanceof Champion c) {
								if (c.isFusion()) showAvailable.set(false);
								return c.getMana();
							} else if (d instanceof Equipment e) {
								showAvailable.set(false);
								return e.getTier();
							} else {
								showAvailable.set(false);
								return 1;
							}
						})
						.reversed()
						.thenComparing(d -> d.getCard().getName(), String.CASE_INSENSITIVE_ORDER)
					)
					.collect(Collectors.toList());

		List<List<Drawable>> chunks = Helper.chunkify(cards, COLUMN_COUNT);
		chunks.removeIf(List::isEmpty);

		NContract<BufferedImage> act = new NContract<>(chunks.size());
		act.setAction(imgs -> {
			BufferedImage bg = new BufferedImage(header.getWidth(), header.getHeight() + footer.getHeight() + (299 * imgs.size()), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g.drawImage(header, 0, 0, null);
			g.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 72));
			Profile.printCenteredString(title, 4026, 35, 168, g);

			for (int i = 0; i < imgs.size(); i++) {
				g.drawImage(imgs.get(i), 0, header.getHeight() + 299 * i, null);
			}

			g.drawImage(footer, 0, bg.getHeight() - footer.getHeight(), null);
			g.dispose();

			return bg;
		});

		Set<String> allCards = CardDAO.getCollectedCardNames(acc.getUid());
		ExecutorService th = Executors.newFixedThreadPool(5);
		for (int c = 0; c < chunks.size(); c++) {
			int finalC = c;
			th.execute(() -> {
				BufferedImage bi = new BufferedImage(row.getWidth(), row.getHeight(), row.getType());
				Graphics2D g = bi.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.drawImage(row, 0, 0, null);

				List<Drawable> chunk = chunks.get(finalC);
				for (int i = 0; i < chunk.size(); i++) {
					Drawable d = chunk.get(i);
					boolean has = !showAvailable.get() || allCards.contains(d.getCard().getId());

					BufferedImage card;
					if (!has) d.setAvailable(false);
					card = d.drawCard(false);

					int width = 4026 / COLUMN_COUNT;
					int actualWidth = width * chunk.size();
					int x = 35 + ((4026 - actualWidth) / 2) + ((width - CARD_WIDTH) / 2) + width * i;

					int height = row.getHeight();
					int y = ((height - CARD_HEIGHT) / 2);

					g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
					g.drawImage(card, x, y, CARD_WIDTH, CARD_HEIGHT, null);
					Profile.printCenteredString(StringUtils.abbreviate(chunk.get(i).getCard().getName(), 15), CARD_WIDTH, x, y + 274, g);
				}

				g.dispose();

				act.addSignature(finalC, bi);
			});
		}

		try {
			return act.get();
		} catch (ExecutionException e) {
			return null;
		} finally {
			th.shutdownNow();
		}
	}
}
