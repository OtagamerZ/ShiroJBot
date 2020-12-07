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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Arena {
	private final Map<Side, List<SlotColumn<Champion, Equipment>>> slots;
	private final Map<Side, LinkedList<Drawable>> graveyard;
	private final LinkedList<Drawable> banished;
	private Field field = null;

	public Arena() {
		this.slots = Map.of(
				Side.TOP, List.of(
						new SlotColumn<>(),
						new SlotColumn<>(),
						new SlotColumn<>(),
						new SlotColumn<>(),
						new SlotColumn<>()
				),
				Side.BOTTOM, List.of(
						new SlotColumn<>(),
						new SlotColumn<>(),
						new SlotColumn<>(),
						new SlotColumn<>(),
						new SlotColumn<>()
				)
		);
		this.graveyard = Map.of(
				Side.TOP, new LinkedList<>(),
				Side.BOTTOM, new LinkedList<>()
		);
		this.banished = new LinkedList<>();
	}

	public Map<Side, List<SlotColumn<Champion, Equipment>>> getSlots() {
		return slots;
	}

	public Map<Side, LinkedList<Drawable>> getGraveyard() {
		return graveyard;
	}

	public LinkedList<Drawable> getBanished() {
		return banished;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public BufferedImage render(Map<Side, Hand> hands) {
		try {
			BufferedImage back = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/backdrop.jpg")));
			BufferedImage arena = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/arenas/" + (field == null ? "default" : field.getField().name().toLowerCase()) + ".png")));
			BufferedImage frames = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/frames.png")));

			Graphics2D g2d = back.createGraphics();
			g2d.drawImage(arena, 0, 0, null);

			slots.forEach((key, value) -> {
				Hand h = hands.get(key);
				Account acc = AccountDAO.getAccount(hands.get(key).getUser().getId());
				LinkedList<Drawable> grv = graveyard.get(key);
				g2d.setColor(Color.white);
				g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 100));

				Profile.printCenteredString(StringUtils.abbreviate(hands.get(key).getUser().getName(), 18), 626, key == Side.TOP ? 1125 : 499, key == Side.TOP ? 838 : 975, g2d);

				for (int i = 0; i < value.size(); i++) {
					SlotColumn<Champion, Equipment> c = value.get(i);
					switch (key) {
						case TOP -> {
							if (c.getTop() != null) {
								Champion d = c.getTop();
								g2d.drawImage(d.drawCard(acc, d.isFlipped()), 499 + (257 * i), 387, null);
							}
							if (c.getBottom() != null) {
								Equipment d = c.getBottom();
								g2d.drawImage(d.drawCard(acc, d.isFlipped()), 499 + (257 * i), 0, null);
							}
							if (grv.size() > 0)
								g2d.drawImage(grv.peekLast().drawCard(acc, false), 1889, 193, null);
							if (h.getDeque().size() > 0) {
								Drawable d = h.getDeque().peek();
								assert d != null;
								g2d.drawImage(d.drawCard(acc, true), 137, 193, null);
							}
							if (h.getLockTime() > 0) {
								try {
									BufferedImage lock = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/locked.png")));
									g2d.drawImage(lock, 137, 193, null);
								} catch (IOException ignore) {
								}
							}
						}
						case BOTTOM -> {
							if (c.getTop() != null) {
								Champion d = c.getTop();
								g2d.drawImage(d.drawCard(acc, d.isFlipped()), 499 + (257 * i), 1013, null);
							}
							if (c.getBottom() != null) {
								Equipment d = c.getBottom();
								g2d.drawImage(d.drawCard(acc, d.isFlipped()), 499 + (257 * i), 1400, null);
							}
							if (grv.size() > 0)
								g2d.drawImage(grv.peekLast().drawCard(acc, false), 137, 1206, null);
							if (h.getDeque().size() > 0) {
								Drawable d = h.getDeque().peek();
								assert d != null;
								g2d.drawImage(d.drawCard(acc, true), 1889, 1206, null);
							}
							if (h.getLockTime() > 0) {
								try {
									BufferedImage lock = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/locked.png")));
									g2d.drawImage(lock, 1889, 1206, null);
								} catch (IOException ignore) {
								}
							}
						}
					}

					float prcnt = h.getHp() / 5000f;
					g2d.setColor(prcnt > 0.75d ? Color.green : prcnt > 0.5d ? Color.yellow : Color.red);
					g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 75));
					g2d.drawString("HP: " + h.getHp(), key == Side.TOP ? 10 : 2240 - g2d.getFontMetrics().stringWidth("HP: " + h.getHp()), key == Side.TOP ? 82 : 1638);
					g2d.setColor(Color.cyan);
					g2d.drawString("MP: " + h.getMana(), key == Side.TOP ? 10 : 2240 - g2d.getFontMetrics().stringWidth("MP: " + h.getMana()), key == Side.TOP ? 178 : 1735);
				}
			});

			if (field != null) {
				g2d.drawImage(field.drawCard(field.getAcc(), false), 1889, 700, null);
			}

			if (banished.peekLast() != null) {
				Drawable d = banished.peekLast();
				g2d.drawImage(d.drawCard(d.getAcc(), false), 137, 700, null);
			}

			g2d.drawImage(frames, 0, 0, null);
			g2d.dispose();

			return back;
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}
}
