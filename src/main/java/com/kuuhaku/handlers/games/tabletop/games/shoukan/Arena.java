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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Arena {
	private final Map<Side, List<SlotColumn<Champion, Equipment>>> slots;
	private final Map<Side, LinkedList<Drawable>> graveyard;
	private final LinkedList<Drawable> banished;
	private Field field = null;

	public Arena() {
		this.slots = Map.of(
				Side.TOP, List.of(
						new SlotColumn<>(0),
						new SlotColumn<>(1),
						new SlotColumn<>(2),
						new SlotColumn<>(3),
						new SlotColumn<>(4)
				),
				Side.BOTTOM, List.of(
						new SlotColumn<>(0),
						new SlotColumn<>(1),
						new SlotColumn<>(2),
						new SlotColumn<>(3),
						new SlotColumn<>(4)
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

	public BufferedImage render(Shoukan game, Map<Side, Hand> hands) {
		try {
			BufferedImage back = Helper.getResourceAsImage(this.getClass(), "shoukan/backdrop.jpg");
			BufferedImage arena = Helper.getResourceAsImage(this.getClass(), "shoukan/arenas/" + (field == null ? "default" : field.getField().toLowerCase(Locale.ROOT)) + ".png");

			assert back != null;
			assert arena != null;
			Graphics2D g2d = back.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g2d.drawImage(arena, 0, 0, null);

			for (Map.Entry<Side, List<SlotColumn<Champion, Equipment>>> entry : slots.entrySet()) {
				Side key = entry.getKey();
				List<SlotColumn<Champion, Equipment>> value = entry.getValue();
				Hand h = hands.get(key);
				LinkedList<Drawable> grv = graveyard.get(key);
				g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 75));

				String name;
				if (game.getClans() != null) {
					name = game.getClans().get(key).getName();
				} else {
					if (h instanceof TeamHand) {
						name = ((TeamHand) h).getNames().stream().map(n -> StringUtils.abbreviate(n, 16)).collect(Collectors.collectingAndThen(Collectors.toList(), Helper.properlyJoin()));
					} else {
						name = StringUtils.abbreviate(h.getUser().getName(), 32);
					}
				}

				if (key == game.getCurrentSide()) {
					g2d.setColor(h.getAcc().getFrame().getColor());
					name = ">>> " + name + " <<<";
				} else {
					g2d.setColor(Color.white);
				}

				if (key == Side.TOP)
					Profile.printCenteredString(name, 1253, 499, 822, g2d);
				else
					Profile.printCenteredString(name, 1253, 499, 1003, g2d);

				for (int i = 0; i < value.size(); i++) {
					SlotColumn<Champion, Equipment> c = value.get(i);
					switch (key) {
						case TOP -> {
							if (c.getTop() != null) {
								Champion d = c.getTop();
								g2d.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 387, null);

								if (!d.isFlipped()) {
									if (d.isBuffed())
										g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/buffed.png"), 489 + (257 * i), 377, null);
									else if (d.isNerfed())
										g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/nerfed.png"), 489 + (257 * i), 377, null);
								}
							}
							if (c.getBottom() != null) {
								Equipment d = c.getBottom();
								g2d.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 0, null);
							}
						}
						case BOTTOM -> {
							if (c.getTop() != null) {
								Champion d = c.getTop();
								g2d.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 1013, null);

								if (!d.isFlipped()) {
									if (d.isBuffed())
										g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/buffed.png"), 489 + (257 * i), 1003, null);
									else if (d.isNerfed())
										g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/nerfed.png"), 489 + (257 * i), 1003, null);
								}
							}
							if (c.getBottom() != null) {
								Equipment d = c.getBottom();
								g2d.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 1400, null);
							}
						}
					}

					float prcnt = (float) h.getHp() / h.getBaseHp();
					g2d.setColor(prcnt > 2 / 3f ? Color.green : prcnt > 1 / 3f ? Color.yellow : Color.red);
					g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 75));

					String hp = StringUtils.leftPad(String.valueOf(h.getHp()), 4, "0");
					String mp = h.isNullMode() ? "--" : StringUtils.leftPad(String.valueOf(h.getMana()), 2, "0");

					Profile.drawOutlinedText(
							"HP: " + hp,
							key == Side.TOP ? 10 : 2240 - g2d.getFontMetrics().stringWidth("MP: " + hp),
							key == Side.TOP ? 82 : 1638, g2d
					);
					g2d.setColor(h.isNullMode() ? new Color(88, 0, 255) : Color.cyan);
					Profile.drawOutlinedText(
							"MP: " + mp,
							key == Side.TOP ? 10 : 2240 - g2d.getFontMetrics().stringWidth("MP: " + mp),
							key == Side.TOP ? 178 : 1735, g2d
					);

					g2d.setColor(Color.white);
					if (grv.size() > 0) {
						g2d.drawImage(grv.peekLast().drawCard(false),
								key == Side.TOP ? 1889 : 137,
								key == Side.TOP ? 193 : 1206, null);
						Profile.printCenteredString("%s/%s/%s".formatted(
								StringUtils.leftPad(String.valueOf(grv.stream().filter(d -> d instanceof Champion).count()), 2, "0"),
								StringUtils.leftPad(String.valueOf(grv.stream().filter(d -> d instanceof Equipment).count()), 2, "0"),
								StringUtils.leftPad(String.valueOf(grv.stream().filter(d -> d instanceof Field).count()), 2, "0")
								), 225,
								key == Side.TOP ? 1889 : 137,
								key == Side.TOP ? 178 : 1638, g2d);
					}

					if (h.getDeque().size() > 0) {
						Drawable d = h.getDeque().peek();
						assert d != null;
						g2d.drawImage(d.drawCard(true),
								key == Side.TOP ? 137 : 1889,
								key == Side.TOP ? 193 : 1206, null);

						Pair<Race, Race> combo = h.getCombo();
						if (combo.getLeft() != Race.NONE)
							g2d.drawImage(combo.getLeft().getIcon(),
									key == Side.TOP ? 137 : 1889,
									key == Side.TOP ? 543 : 1078, 128, 128, null);
						if (combo.getRight() != Race.NONE)
							g2d.drawImage(combo.getRight().getIcon(),
									key == Side.TOP ? 284 : 2036,
									key == Side.TOP ? 568 : 1103, 78, 78, null);
					}

					if (h.getLockTime() > 0) {
						BufferedImage lock = Helper.getResourceAsImage(this.getClass(), "shoukan/locked.png");
						g2d.drawImage(lock,
								key == Side.TOP ? 137 : 1889,
								key == Side.TOP ? 193 : 1206, null);
					}
				}
			}

			if (field != null) {
				g2d.drawImage(field.drawCard(false), 1889, 700, null);
			}

			if (banished.peekLast() != null) {
				Drawable d = banished.peekLast();
				g2d.drawImage(d.drawCard(false), 137, 700, null);
			}

			Map<String, Integer> locks = Map.of(
					"fusion", game.getFusionLock(),
					"spell", game.getSpellLock(),
					"effect", game.getEffectLock()
			);

			String[] lockNames = {
					"fusion",
					"spell",
					"effect"
			};

			g2d.setColor(Color.red);
			for (int i = 0; i < lockNames.length; i++) {
				String name = locks.get(lockNames[i]) > 0 ? lockNames[i] + "_lock" : lockNames[i] + "_unlock";
				BufferedImage icon;
				icon = Helper.getResourceAsImage(this.getClass(), "shoukan/" + name + ".png");
				g2d.drawImage(icon, 919 + (i * 166), 835, null);
				if (locks.get(lockNames[i]) > 0)
					Profile.drawOutlinedText(String.valueOf(locks.get(lockNames[i])), 1009 + (i * 166), 860 + g2d.getFontMetrics().getHeight() / 2, g2d);
			}

			g2d.dispose();

			return back;
		} catch (NullPointerException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public BufferedImage addHands(BufferedImage arena, Collection<Hand> hands) {
		List<Hand> hs = new ArrayList<>(hands);
		hs.sort(Comparator.comparingInt(h -> h.getSide() == Side.TOP ? 1 : 0));
		BufferedImage bi = new BufferedImage(arena.getWidth(), arena.getHeight() + 920, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = arena.createGraphics();
		g2d.setColor(Color.black);
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.drawImage(arena, 0, 460, null);
		for (int i = 0; i < hs.size(); i++) {
			BufferedImage h = hs.get(i).render();
			g2d.drawImage(h, bi.getWidth() / 2 - h.getWidth() / 2, i == 0 ? 5 : 465 + bi.getHeight(), null);
		}
		g2d.dispose();

		return bi;
	}
}
