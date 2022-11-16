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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.utils.BondedList;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.NContract;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.ON_GRAVEYARD;
import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.ON_SACRIFICE;

public class Arena {
	private final Map<Side, List<SlotColumn>> slots;
	private final Map<Side, BondedList<Drawable>> graveyard;
	private final BondedList<Drawable> banned;
	private Field field = null;
	private boolean updateField = true;

	public Arena(Shoukan game) {
		Hand top = game.getHands().get(Side.TOP);
		Hand bot = game.getHands().get(Side.BOTTOM);

		this.slots = Map.of(
				Side.TOP, List.of(
						new SlotColumn(0, game, top),
						new SlotColumn(1, game, top),
						new SlotColumn(2, game, top),
						new SlotColumn(3, game, top),
						new SlotColumn(4, game, top)
				),
				Side.BOTTOM, List.of(
						new SlotColumn(0, game, bot),
						new SlotColumn(1, game, bot),
						new SlotColumn(2, game, bot),
						new SlotColumn(3, game, bot),
						new SlotColumn(4, game, bot)
				)
		);
		this.graveyard = Map.of(
				Side.TOP, new BondedList<>(d -> {
					if (game.getCurrentSide() == Side.TOP) {
						game.applyEffect(ON_SACRIFICE, d, Side.TOP, d.getIndex());
					}

					game.applyEffect(ON_GRAVEYARD, d, Side.TOP, d.getIndex());

					d.reset();
				}),
				Side.BOTTOM, new BondedList<>(d -> {
					if (game.getCurrentSide() == Side.BOTTOM) {
						game.applyEffect(ON_SACRIFICE, d, Side.BOTTOM, d.getIndex());
					}

					game.applyEffect(ON_GRAVEYARD, d, Side.BOTTOM, d.getIndex());

					d.reset();
				})
		);
		this.banned = new BondedList<>(Drawable::reset);
	}

	public Map<Side, List<SlotColumn>> getSlots() {
		return slots;
	}

	public Map<Side, BondedList<Drawable>> getGraveyard() {
		return graveyard;
	}

	public BondedList<Drawable> getBanned() {
		return banned;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
		this.updateField = true;
	}

	public BufferedImage render(Shoukan game, Map<Side, Hand> hands) {
		try {
			BufferedImage bi = Helper.getResourceAsImage(this.getClass(), "shoukan/backdrop.jpg");

			assert bi != null;
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			BufferedImage arena = Helper.getResourceAsImage(this.getClass(), "shoukan/arenas/" + (field == null ? "default" : field.getField().toLowerCase(Locale.ROOT)) + ".png");

			assert arena != null;
			g2d.drawImage(arena, 0, 0, null);
			updateField = false;
			g2d.dispose();

			NContract<BufferedImage> sides = new NContract<>(2, imgs -> {
				Graphics2D g = arena.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

				g.setComposite(AlphaComposite.Clear);
				g.fillRect(0, 0, arena.getWidth(), arena.getHeight());
				g.setComposite(AlphaComposite.SrcOver);

				for (BufferedImage img : imgs) {
					g.drawImage(img, 0, 0, null);
				}
				g.dispose();

				return arena;
			});

			ExecutorService exec = Executors.newFixedThreadPool(2);
			for (Map.Entry<Side, List<SlotColumn>> entry : slots.entrySet()) {
				exec.execute(() -> {
					BufferedImage layer = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = layer.createGraphics();

					Side key = entry.getKey();
					List<SlotColumn> value = entry.getValue();
					Hand h = hands.get(key);
					LinkedList<Drawable> grv = graveyard.get(key);
					g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 75));

					String name;
					if (h instanceof TeamHand th) {
						name = Helper.parseAndJoin(th.getNames(), n -> StringUtils.abbreviate(n, 16));
					} else {
						name = StringUtils.abbreviate(h.getUser().getName(), 32);
					}

					if (key == game.getCurrentSide()) {
						FrameColor fc = h.getAcc().getFrame();

						g.setColor(fc.getThemeColor());
						g.setBackground(fc.getBackgroundColor());

						name = ">>> " + name + " <<<";
					} else {
						g.setColor(Color.white);
					}

					if (key == Side.TOP)
						Profile.printCenteredString(name, 1253, 499, 822, g);
					else
						Profile.printCenteredString(name, 1253, 499, 998, g);

					g.setBackground(Color.BLACK);
					BufferedImage broken = Helper.getResourceAsImage(this.getClass(), "shoukan/broken.png");
					for (int i = 0; i < value.size(); i++) {
						SlotColumn c = value.get(i);
						switch (key) {
							case TOP -> {
								if (c.isUnavailable()) {
									g.drawImage(broken, 499 + (257 * i), 387, null);
								} else if (c.getTop() != null) {
									Champion d = c.getTop();
									g.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 387, null);

									String path = d.getAcc().getFrame().name().startsWith("LEGACY_") ? "old" : "new";
									if (!d.isFlipped()) {
										if (d.isBuffed())
											g.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/buffed.png"), 484 + (257 * i), 372, null);
										else if (d.isNerfed())
											g.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/nerfed.png"), 484 + (257 * i), 372, null);
										else if (d.getHero() != null)
											g.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/hero.png"), 484 + (257 * i), 372, null);
									}

									if (d.getHero() != null) {
										g.setColor(Color.orange);
										g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
										Profile.printCenteredString("HP: " + d.getHero().getHitpoints(), 257, 484 + (257 * i), 377, g);
									}
								}

								if (c.isUnavailable()) {
									g.drawImage(broken, 499 + (257 * i), 0, null);
								} else if (c.getBottom() != null) {
									Equipment d = c.getBottom();
									g.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 0, null);
								}
							}
							case BOTTOM -> {
								if (c.isUnavailable()) {
									g.drawImage(broken, 499 + (257 * i), 1013, null);
								} else if (c.getTop() != null) {
									Champion d = c.getTop();
									g.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 1013, null);

									String path = d.getAcc().getFrame().name().startsWith("LEGACY_") ? "old" : "new";
									if (!d.isFlipped()) {
										if (d.isBuffed())
											g.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/buffed.png"), 484 + (257 * i), 998, null);
										else if (d.isNerfed())
											g.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/nerfed.png"), 484 + (257 * i), 998, null);
										else if (d.getHero() != null)
											g.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/hero.png"), 484 + (257 * i), 998, null);
									}

									if (d.getHero() != null) {
										g.setColor(Color.orange);
										g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
										Profile.printCenteredString("HP: " + d.getHero().getHitpoints(), 257, 484 + (257 * i), 1390, g);
									}
								}

								if (c.isUnavailable()) {
									g.drawImage(broken, 499 + (257 * i), 1400, null);
								} else if (c.getBottom() != null) {
									Equipment d = c.getBottom();
									g.drawImage(d.drawCard(d.isFlipped()), 499 + (257 * i), 1400, null);
								}
							}
						}

						float prcnt = (float) h.getHp() / h.getBaseHp();
						g.setColor(prcnt > 2 / 3f ? Color.green : prcnt > 1 / 3f ? Color.yellow : Color.red);
						g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 65));

						String hp = "HP: %04d".formatted(Math.max(0, h.getHp()));
						String mp = h.isHidingMana() ? "MP: --" : "MP: %02d".formatted(Math.max(0, h.getMana()));

						int hpWidth = g.getFontMetrics().stringWidth(hp);
						Profile.drawOutlinedText(
								hp,
								key == Side.TOP ? 10 : 2240 - hpWidth,
								key == Side.TOP ? 82 : 1638, g
						);

						if (h.getBleeding() > 0) {
							g.setColor(new Color(153, 0, 0));
							Profile.drawOutlinedText(
									"(-" + h.getBleeding() + ")",
									key == Side.TOP ? hpWidth + 10 : (2230 - hpWidth) - g.getFontMetrics().stringWidth("(-" + h.getBleeding() + ")"),
									key == Side.TOP ? 82 : 1638, g
							);
						} else if (h.getRegeneration() > 0) {
							g.setColor(new Color(0, 153, 89));
							Profile.drawOutlinedText(
									"(+" + h.getRegeneration() + ")",
									key == Side.TOP ? hpWidth + 10 : (2230 - hpWidth) - g.getFontMetrics().stringWidth("(+" + h.getRegeneration() + ")"),
									key == Side.TOP ? 82 : 1638, g
							);
						}

						g.setColor(h.isNullMode() ? new Color(88, 0, 255) : Color.cyan);
						Profile.drawOutlinedText(
								mp,
								key == Side.TOP ? 10 : 2240 - g.getFontMetrics().stringWidth(mp),
								key == Side.TOP ? 168 : 1725, g
						);

						g.setColor(Color.white);
						if (grv.size() > 0) {
							g.drawImage(grv.peekLast().drawCard(false),
									key == Side.TOP ? 1889 : 137,
									key == Side.TOP ? 193 : 1206, null);

							Integer[] count = {0, 0, 0};
							for (Drawable d : grv) {
								if (d instanceof Champion) count[0]++;
								else if (d instanceof Equipment) count[1]++;
								else count[2]++;
							}

							Profile.printCenteredString("%02d/%02d/%02d".formatted((Object[]) count), 225, key == Side.TOP ? 1889 : 137, key == Side.TOP ? 178 : 1638, g);
						}

						if (h.getRealDeque().size() > 0) {
							Drawable d = h.getRealDeque().peek();
							assert d != null;
							g.drawImage(d.drawCard(true),
									key == Side.TOP ? 137 : 1889,
									key == Side.TOP ? 193 : 1206, null);

							Triple<Race, Boolean, Race> combo = h.getCombo();
							if (combo.getLeft() != Race.NONE)
								g.drawImage(combo.getLeft().getIcon(),
										key == Side.TOP ? 137 : 1889,
										key == Side.TOP ? 543 : 1078, 128, 128, null);
							if (combo.getRight() != Race.NONE)
								g.drawImage(combo.getRight().getIcon(),
										key == Side.TOP ? 284 : 2036,
										key == Side.TOP ? 568 : 1103, 78, 78, null);
						}

						if (h.getLockTime() > 0) {
							BufferedImage lock = Helper.getResourceAsImage(this.getClass(), "shoukan/locked.png");
							g.drawImage(lock,
									key == Side.TOP ? 137 : 1889,
									key == Side.TOP ? 193 : 1206, null);
						}
					}

					g.dispose();

					sides.addSignature(key.ordinal(), layer);
				});
			}

			Graphics2D g = sides.get().createGraphics();
			if (field != null) {
				g.drawImage(field.drawCard(false), 1889, 700, null);
			}

			if (banned.peekLast() != null) {
				Drawable d = banned.peekLast();
				g.drawImage(d.drawCard(false), 137, 700, null);
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

			g.setColor(Color.red);
			g.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 65));
			for (int i = 0; i < lockNames.length; i++) {
				String name = locks.get(lockNames[i]) > 0 ? lockNames[i] + "_lock" : lockNames[i] + "_unlock";
				BufferedImage icon;
				icon = Helper.getResourceAsImage(this.getClass(), "shoukan/" + name + ".png");
				g.drawImage(icon, 919 + (i * 166), 835, null);
				if (locks.get(lockNames[i]) > 0)
					Profile.drawOutlinedText(String.valueOf(locks.get(lockNames[i])), 1009 + (i * 166), 860 + g.getFontMetrics().getHeight() / 2, g);
			}

			g.dispose();

			return arena;
		} catch (NullPointerException | InterruptedException | ExecutionException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return null;
		}
	}

	public BufferedImage renderSide(Side side) {
		BufferedImage bi = new BufferedImage(1317, 796, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

		int evos = side == Side.TOP ? 32 : 382;
		int champs = side == Side.TOP ? 382 : 32;

		List<SlotColumn> slts = slots.get(side);
		for (SlotColumn slt : slts) {
			Champion c = slt.getTop();
			if (c != null) {
				g2d.drawImage(c.drawCard(false), 32 + (257 * slt.getIndex()), champs, null);

				String path = c.getAcc().getFrame().name().startsWith("LEGACY_") ? "old" : "new";
				if (c.isBuffed())
					g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/buffed.png"), 17 + (257 * slt.getIndex()), champs - 15, null);
				else if (c.isNerfed())
					g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/nerfed.png"), 17 + (257 * slt.getIndex()), champs - 15, null);
				else if (c.getHero() != null)
					g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "kawaipon/frames/" + path + "/hero.png"), 17 + (257 * slt.getIndex()), champs - 15, null);

				if (c.getHero() != null) {
					g2d.setColor(Color.orange);
					g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
					Profile.printCenteredString("HP: " + c.getHero().getHitpoints(), 257, 47 + (257 * slt.getIndex()), champs + 10, g2d);
				}
			}

			Equipment e = slt.getBottom();
			if (e != null) {
				g2d.drawImage(e.drawCard(false), 32 + (257 * slt.getIndex()), evos, null);
			}
		}

		g2d.dispose();

		return bi;
	}

	public BufferedImage addHands(BufferedImage arena, Collection<Hand> hands) {
		List<Hand> hs = new ArrayList<>(hands);
		hs.sort(Comparator.comparingInt(h -> h.getSide() == Side.TOP ? 1 : 0));

		BufferedImage bi = new BufferedImage(arena.getWidth(), arena.getHeight() + 740, arena.getType());
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(Color.black);
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.drawImage(arena, 0, 370, null);
		for (int i = 0; i < hs.size(); i++) {
			BufferedImage h = hs.get(i).render();
			h = Helper.scaleAndCenterImage(h, bi.getWidth(), h.getHeight());

			g2d.drawImage(h, bi.getWidth(), i == 0 ? 370 + arena.getHeight() + 10 : 10, null);
		}
		g2d.dispose();

		return bi;
	}
}
