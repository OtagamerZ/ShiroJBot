/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.game.engine.Renderer;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.interfaces.shoukan.Proxy;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.*;
import com.kuuhaku.model.records.shoukan.HistoryLog;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.model.records.shoukan.Timed;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Arena implements Renderer {
	private final Point MARGIN = new Point(25, 25);
	public final Dimension SIZE = new Dimension(
			(225 + MARGIN.x * 2) * 5 /* slots */ + (225 + MARGIN.x * 2) * 4 /* side stacks */,
			(350 + MARGIN.y) * 4 /* slots */ + MARGIN.y * 10 - 1
	);
	private final Point CENTER = new Point(SIZE.width / 2, SIZE.height / 2);
	private final Dimension BAR_SIZE = new Dimension(SIZE.width / 2, 100);
	/*
              ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐
              │  │ │  │ │  │ │  │ │  │
    ┌──┐      └──┘ └──┘ └──┘ └──┘ └──┘      ┌──┐
    │  │      ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐      │  │
    └──┘      │  │ │  │ │  │ │  │ │  │      └──┘
	┌──┐      └──┘ └──┘ └──┘ └──┘ └──┘      ┌──┐
	│  │      ---------- 50 ----------      │  │
	└──┘      ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐      └──┘
	┌──┐      │  │ │  │ │  │ │  │ │  │      ┌──┐
    │  │      └──┘ └──┘ └──┘ └──┘ └──┘      │  │
    └──┘      ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐      └──┘
              │  │ │  │ │  │ │  │ │  │
              └──┘ └──┘ └──┘ └──┘ └──┘
	*/

	private final Shoukan game;
	private final Map<Side, List<SlotColumn>> slots;
	private final BondedList<Drawable<?>> banned = new BondedList<>((d, it) -> {
		if (d.getHand() == null) return false;
		else if (d.isEthereal() || getBanned().contains(d)) return false;

		if (d instanceof Proxy<?> p && !(p instanceof Senshi)) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		getGame().trigger(Trigger.ON_BAN, d.asSource(Trigger.ON_BAN));

		if (d instanceof Senshi s) {
			if (!s.getEquipments().isEmpty()) {
				for (Evogear evogear : s.getEquipments()) {
					it.add(evogear);
				}
			}
		} else if (d instanceof Evogear e && e.getEquipper() != null) {
			e.getEquipper().getEquipments().remove(e);
		}

		d.reset();

		if (d instanceof Proxy<?> p) {
			d.reset();
			it.add(p.getOriginal());
			return false;
		}

		return !(d instanceof EffectHolder<?> eh) || !eh.hasFlag(Flag.BOUND, true);
	});

	public final Field DEFAULT_FIELD = DAO.find(Field.class, "DEFAULT");
	private Field field = null;

	public Arena(Shoukan game) {
		this.game = game;
		slots = Map.of(
				Side.TOP, Utils.generate(5, i -> new SlotColumn(game, Side.TOP, i)),
				Side.BOTTOM, Utils.generate(5, i -> new SlotColumn(game, Side.BOTTOM, i))
		);
	}

	public Shoukan getGame() {
		return game;
	}

	public Map<Side, List<SlotColumn>> getSlots() {
		return slots;
	}

	public List<SlotColumn> getSlots(Side side) {
		return slots.get(side);
	}

	public boolean isFieldEmpty(Side side) {
		boolean sentinel = game.getHands().get(side).getOrigins().synergy() == Race.SENTINEL;
		return slots.get(side).stream().allMatch(sc -> sc.getTop() == null && (!sentinel || sc.getBottom() == null));
	}

	public BondedList<Drawable<?>> getBanned() {
		banned.removeIf(d -> !d.keepOnDestroy());

		return banned;
	}

	public Field getField() {
		return Utils.getOr(field, DEFAULT_FIELD);
	}

	public void setField(Field field) {
		if (Objects.equals(this.field, field)) return;
		this.field = field;

		if (field.getHand() == null) {
			field.setHand(game.getCurrent());
		}

		game.trigger(Trigger.ON_FIELD_CHANGE);
	}

	@Override
	public BufferedImage render(I18N locale) {
		Hand top = game.getHands().get(Side.TOP);
		Hand bottom = game.getHands().get(Side.BOTTOM);

		BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height + BAR_SIZE.height * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		Graph.applyTransformed(g2d, 0, BAR_SIZE.height, g1 -> {
			g1.drawImage(getField().renderBackground(), 0, 0, null);

			for (Side side : Side.values()) {
				int xOffset = CENTER.x - ((225 + MARGIN.x) * 5 - MARGIN.x) / 2;
				int yOffset = switch (side) {
					case TOP -> CENTER.y - (350 + MARGIN.y) * 2 - MARGIN.y * 4;
					case BOTTOM -> CENTER.y + MARGIN.y * 5;
				};

				Hand h = game.getHands().get(side);
				int regdeg = h.getRegDeg().peek();
				if (regdeg != 0) {
					BufferedImage over = IO.getResourceAsImage("shoukan/overlay/" + (regdeg > 0 ? "r" : "d") + "egen_" + side.name().toLowerCase() + ".png");
					g1.drawImage(over, 0, CENTER.y * side.ordinal(), null);
				}

				Deck deck = h.getUserDeck();
				DeckStyling style = deck.getStyling();

				g1.drawImage(deck.getSkin().getImage(side, style.getFrame().isLegacy()), 21, yOffset - 5, null);

				Graph.applyTransformed(g1, xOffset, yOffset, g2 -> {
					for (SlotColumn slot : slots.get(side)) {
						int x = (225 + MARGIN.x) * slot.getIndex() - 15;
						int equips, frontline, backline;

						if (side == Side.TOP) {
							equips = 350 * 2 + MARGIN.y + MARGIN.y / 4 - 5;
							frontline = 350 + MARGIN.y - 15;
							backline = -15;
						} else {
							equips = -350 / 3 - MARGIN.y / 4 - 5;
							frontline = -15;
							backline = 350 + MARGIN.y - 15;
						}

						if (slot.isLocked()) {
							BufferedImage hole = IO.getResourceAsImage("shoukan/states/broken.png");
							g2.drawImage(hole, x + 15, frontline + 15, null);
							g2.drawImage(hole, x + 15, backline + 15, null);
						} else {
							if (slot.hasTop()) {
								Senshi s = slot.getTop();

								g2.drawImage(s.render(locale, deck), x, frontline, null);

								if (!s.getEquipments().isEmpty()) {
									Graph.applyTransformed(g2, x, equips, g3 -> {
										Dimension resized = new Dimension(Drawable.SIZE.width / 3, Drawable.SIZE.height / 3);
										int middle = 225 / 2 - resized.width / 2;

										for (int i = 0; i < s.getEquipments().size(); i++) {
											g3.drawImage(s.getEquipments().get(i).render(locale, deck),
													15 + middle + (resized.width - 5) * (i - 1), 0,
													resized.width, resized.height,
													null
											);
										}
									});
								}
							}

							if (slot.hasBottom()) {
								g2.drawImage(slot.getBottom().render(locale, deck), x, backline, null);
							}
						}
					}
				});
			}

			Graph.applyTransformed(g1, MARGIN.x, CENTER.y - Drawable.SIZE.height / 2, g2 -> {
				if (!top.getRealDeck().isEmpty()) {
					Deck d = top.getUserDeck();
					g2.drawImage(d.getStyling().getFrame().getBack(d),
							0, 15 - (350 + MARGIN.y), null
					);
				}
				if (!banned.isEmpty()) {
					Drawable<?> d = banned.getLast();
					g2.drawImage(d.render(locale, d.getHand().getUserDeck()),
							-15, 0, null
					);
				}
				if (!bottom.getGraveyard().isEmpty()) {
					Drawable<?> d = bottom.getGraveyard().getLast();
					g2.drawImage(d.render(locale, bottom.getUserDeck()),
							-15, 350 + MARGIN.y, null
					);
				}
			});

			Graph.applyTransformed(g1, SIZE.width - Drawable.SIZE.width - MARGIN.x, CENTER.y - Drawable.SIZE.height / 2, g2 -> {
				if (!top.getGraveyard().isEmpty()) {
					Drawable<?> d = top.getGraveyard().getLast();
					g2.drawImage(d.render(locale, top.getUserDeck()),
							15, -(350 + MARGIN.y), null
					);
				}
				if (!getField().getId().equals("DEFAULT")) {
					g2.drawImage(getField().render(locale, Utils.getOr(() -> getField().getHand().getUserDeck(), Deck.INSTANCE)),
							15, 0, null
					);
				}
				if (!bottom.getRealDeck().isEmpty()) {
					Deck d = bottom.getUserDeck();
					g2.drawImage(d.getStyling().getFrame().getBack(d),
							30, 15 + 350 + MARGIN.y, null
					);
				}
			});
		});

		Graph.applyTransformed(g2d, drawBar(top));

		Graph.applyTransformed(g2d, drawBar(bottom));

		return bi;
	}

	@Override
	public BufferedImage render(I18N locale, Deque<HistoryLog> history) {
		BufferedImage source = render(locale);

		BufferedImage bi = new BufferedImage((int) (source.getWidth() * 1.33), source.getHeight(), source.getType());
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		g2d.drawImage(source, 0, 0, null);

		Graph.applyTransformed(g2d, source.getWidth(), BAR_SIZE.height, g1 -> {
			int w = source.getWidth() / 3;

			g1.setColor(new Color(0x50000000, true));
			g1.fillRect(0, 0, w, SIZE.height);
			g1.setFont(Fonts.OPEN_SANS.deriveBold((int) (BAR_SIZE.height / 2.5)));
			g1.setClip(new Rectangle(0, 0, SIZE.width, SIZE.height));

			Graph.applyTransformed(g1, 0, SIZE.height, g2 -> {
				String hist = history.stream()
						.map(log -> "{" + log.side().name() + "}" + log.message())
						.collect(Collectors.joining("\n\n"));

				int h = (int) Graph.getMultilineStringBounds(g2, hist, w).getHeight();

				Color top = game.getHands().get(Side.TOP).getUserDeck().getStyling().getFrame().getThemeColor();
				Color bot = game.getHands().get(Side.BOTTOM).getUserDeck().getStyling().getFrame().getThemeColor();

				Graph.drawMultilineString(g2, hist,
						MARGIN.x, -h, w - MARGIN.x, 0,
						str -> {
							boolean clrChange = false;
							if (str.startsWith("{TOP}")) {
								g2.setColor(top);
								clrChange = true;
							} else if (str.startsWith("{BOTTOM}")) {
								g2.setColor(bot);
								clrChange = true;
							}

							return clrChange ? str.substring(str.indexOf("}") + 1) : str;
						},
						(str, px, py) -> Graph.drawOutlinedString(g2, str, px, py, 6, Color.BLACK)
				);
			});
		});

		g2d.dispose();

		return bi;
	}

	public BufferedImage renderEvogears() {
		BufferedImage bi = new BufferedImage((20 + Drawable.SIZE.width * 3) * 5, Drawable.SIZE.height * 2 + 100, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);
		g2d.setStroke(new BasicStroke(2));
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(75));

		int slotWidth = bi.getWidth() / 5;
		int slotHeight = bi.getHeight() / 2;
		g2d.drawLine(0, slotHeight, bi.getWidth(), slotHeight);

		int y = 0;
		for (Side side : Side.values()) {
			List<SlotColumn> slts = slots.get(side);

			for (int x = 0; x < slts.size(); x++) {
				Senshi s = slts.get(x).getTop();
				if (s != null) {
					Graph.applyTransformed(g2d, slotWidth * x, (Drawable.SIZE.height + 100) * y, g1 -> {
						List<Evogear> equips = s.getEquipments();
						for (int i = 0; i < Math.min(equips.size(), 3); i++) {
							g2d.drawImage(equips.get(i).render(game.getLocale(), s.getHand().getUserDeck()),
									10 + Drawable.SIZE.width * i, 0, null
							);
						}
					});
				}

				if (y == 0) {
					Rectangle rect = Graph.getStringBounds(g2d, String.valueOf(x + 1));
					Graph.drawOutlinedString(g2d, String.valueOf(x + 1),
							slotWidth * x + (slotWidth / 2 - rect.width / 2), slotHeight + rect.height / 2,
							6, Color.BLACK
					);
				}

				if (x > 0) {
					g2d.drawLine(slotWidth * x, 0, slotWidth * x, bi.getHeight());
				}
			}

			y++;
		}

		g2d.dispose();

		return bi;
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private Consumer<Graphics2D> drawBar(Hand hand) {
		return g -> {
			boolean reversed = hand.getSide() != Side.TOP;
			boolean demon = hand.getOrigins().major() == Race.DEMON;

			int leftOffset = 250;
			double lOffPercent = (double) leftOffset / BAR_SIZE.width;

			BufferedImage bi = new BufferedImage(BAR_SIZE.width + leftOffset, BAR_SIZE.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHints(Constants.HD_HINTS);

			Graph.applyTransformed(g2d, g1 -> {
				if (reversed) {
					g1.scale(-1, -1);
					g1.translate(-bi.getWidth(), -bi.getHeight());
				}

				double h = 1 - 2d / BAR_SIZE.height;
				double w = 1 - 2d / BAR_SIZE.width;
				Polygon boundaries = Graph.makePoly(BAR_SIZE,
						1 - w, h / 3d,
						w / 30d, 1 - h,
						lOffPercent + w / 1.5, 1 - h,
						lOffPercent + w / 1.4, h / 3d,
						lOffPercent + w / 1.1, h / 3d,
						lOffPercent + w, h,
						1 - w, h
				);
				g1.setColor(hand.getUserDeck().getStyling().getFrame().getThemeColor());
				g1.fill(boundaries);

				g1.setClip(boundaries);

				if (!demon) {
					int mpWidth = (int) (BAR_SIZE.width / 1.4);
					Color manaOver1 = new Color(0x1181FF);
					Color manaOver2 = new Color(0x4D15FF);
					for (int i = 0; i < 33; i++) {
						g1.setColor(Color.CYAN);
						if (hand.getMP() > 66 + i) {
							g1.setColor(manaOver2);
						} else if (hand.getMP() > 33 + i) {
							g1.setColor(manaOver1);
						} else if (hand.getMP() <= i) {
							g1.setColor(Color.DARK_GRAY);
						}

						int width = mpWidth / 33 - 8;
						if (i == 32) {
							width *= 2;
						}

						g1.fillRect(leftOffset + 2 + (mpWidth / 33) * i, 2, width, BAR_SIZE.height / 3 - 2);
					}
				}

				Rectangle bar;
				if (demon) {
					bar = new Rectangle(0, 0, BAR_SIZE.width + leftOffset, BAR_SIZE.height);
				} else {
					bar = new Rectangle(
							leftOffset + 2, (int) (BAR_SIZE.height / 3d + 4 - (reversed ? 2 : 0)),
							BAR_SIZE.width, (int) (BAR_SIZE.height / 1.75)
					);
				}

				g1.setColor(Color.DARK_GRAY);
				g1.fill(bar);

				double fac = hand.getHPPrcnt();
				if (fac >= 0.5 && !hand.getOrigins().demon()) {
					g1.setColor(new Color(69, 173, 28));
				} else if (fac >= 0.25) {
					g1.setColor(new Color(197, 158, 0));
				} else {
					g1.setColor(new Color(173, 28, 28));
				}

				fac = (double) hand.getHP() / hand.getBase().hp();
				g1.fill(new Rectangle(bar.x, bar.y, (int) (bar.width * Math.min(fac, 1)), bar.height));

				if (fac > 1) {
					g1.setColor(new Color(0, 255, 149));
					g1.fill(new Rectangle(bar.x, bar.y, (int) (bar.width * Calc.clamp(fac - 1, 0, 1)), bar.height));
				}

				int regdeg = hand.getRegDeg().peek();
				if (regdeg != 0) {
					BufferedImage tex = IO.getResourceAsImage("shoukan/overlay/" + (regdeg > 0 ? "r" : "d") + "egen.png");
					g1.setPaint(new TexturePaint(
							tex,
							new Rectangle(bar.x, bar.y + bar.height + (reversed ? 1 : 0), bar.height, bar.height * (reversed ? -1 : 1) + (reversed ? -1 : 1))
					));
					g1.fill(new Rectangle(bar.x, bar.y, (int) (bar.width * Math.min((double) Math.abs(regdeg) / hand.getBase().hp(), 1)), bar.height));
				}

				Graph.applyTransformed(g1, reversed ? -1 : 1, g2 -> {
					if (!demon) {
						String mpText = "MP: " + StringUtils.leftPad(String.valueOf(hand.getMP()), 2, "0");
						g2.setColor(Color.CYAN);
						g2.setFont(Fonts.OPEN_SANS_COMPACT.deriveBold(BAR_SIZE.height - 20));

						if (reversed) {
							Graph.drawOutlinedString(g2, mpText,
									-(MARGIN.x + g2.getFontMetrics().stringWidth(mpText)), -20,
									6, Color.BLACK
							);
						} else {
							Graph.drawOutlinedString(g2, mpText,
									MARGIN.x, BAR_SIZE.height - 20,
									6, Color.BLACK
							);
						}
					}

					int pad = Utils.getDigits(hand.getBase().hp());
					String hpText = "HP: "
									+ StringUtils.leftPad(String.valueOf(hand.getHP()), pad, "0")
									+ "/"
									+ StringUtils.leftPad(String.valueOf(hand.getBase().hp()), pad, "0");
					g2.setColor(Color.WHITE);
					g2.setFont(Fonts.OPEN_SANS_BOLD.deriveBold((int) (BAR_SIZE.height / 2.5) * (demon ? 2 : 1)));

					int offset = 6;
					if (demon) offset += 10;

					Color rdColor = regdeg < 0 ? new Color(0xCD0000) : new Color(0x009DFF);
					if (reversed) {
						String rdText = "";
						if (regdeg != 0) {
							rdText = " " + Utils.sign(regdeg);
							g2.setColor(rdColor);
							Graph.drawOutlinedString(g2, rdText,
									-(bar.x + offset + g2.getFontMetrics().stringWidth(rdText)), -(bar.y + offset),
									6, Color.BLACK
							);
						}

						g2.setColor(Color.WHITE);
						Graph.drawOutlinedString(g2, hpText,
								-(bar.x + offset + g2.getFontMetrics().stringWidth(hpText + rdText)), -(bar.y + offset),
								6, Color.BLACK
						);
					} else {
						if (regdeg != 0) {
							String rdText = " " + Utils.sign(regdeg);
							g2.setColor(rdColor);
							Graph.drawOutlinedString(g2, rdText,
									bar.x + offset + g2.getFontMetrics().stringWidth(hpText), bar.y + bar.height - offset,
									6, Color.BLACK
							);
						}

						g2.setColor(Color.WHITE);
						Graph.drawOutlinedString(g2, hpText,
								bar.x + offset, bar.y + bar.height - offset,
								6, Color.BLACK
						);
					}
				});

				g1.setClip(null);
				g1.setColor(hand.getUserDeck().getStyling().getFrame().getThemeColor());
				g1.setStroke(new BasicStroke(5));
				g1.draw(boundaries);
			});

			g2d.dispose();

			int x;
			int y;
			String name = StringUtils.abbreviate(hand.getName(), 20);
			g.setColor(Color.WHITE);
			g.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(BAR_SIZE.height / 3f * 2));
			if (game.getCurrentSide() == hand.getSide()) {
				name = "==> " + name + " <==";
			}

			if (reversed) {
				g.drawImage(bi, SIZE.width - bi.getWidth(), BAR_SIZE.height + SIZE.height, null);

				x = MARGIN.x;
				y = SIZE.height + BAR_SIZE.height + (BAR_SIZE.height + BAR_SIZE.height / 3) / 2 + 10;
			} else {
				g.drawImage(bi, 0, 0, null);

				x = SIZE.width - MARGIN.x - g.getFontMetrics().stringWidth(name);
				y = (BAR_SIZE.height + BAR_SIZE.height / 3) / 2 + 10;
			}

			if (game.getCurrentSide() == hand.getSide()) {
				g.setColor(hand.getUserDeck().getStyling().getFrame().getThemeColor());
			} else {
				g.setColor(Color.WHITE);
			}

			Graph.drawOutlinedString(g, name, x, y, 10, Color.BLACK);

			int rad = (int) (BAR_SIZE.height / 1.5);
			Graph.applyTransformed(g, reversed ? 1860 : 5, BAR_SIZE.height + (reversed ? SIZE.height - rad - 10 : 5), g1 -> {
				int i = 0;
				int yOffset = reversed ? -(rad + 10) : rad + 10;
				for (Timed<Lock> lock : hand.getLocks()) {
					int row = i / 3;

					g1.setColor(Color.RED);
					g1.setFont(Fonts.OPEN_SANS_EXTRABOLD.deriveBold(rad));
					String text = String.valueOf(lock.time());

					FontMetrics fm = g1.getFontMetrics();
					g1.drawImage(lock.obj().getImage(true),
							(rad + 5 + fm.stringWidth("00")) * (i % 3), yOffset * row,
							rad, rad,
							null
					);

					Graph.drawOutlinedString(g1, text,
							(rad + 5 + fm.stringWidth("00")) * (i % 3) + rad + 5, yOffset * row + rad - 5,
							6, Color.BLACK
					);

					i++;
				}
			});

			Graph.applyTransformed(g, reversed ? 2449 : 26, reversed ? 1486 : 208,
					g1 -> {
						Origin ori = hand.getOrigins();

						Rectangle rect = new Rectangle(rad * 2, rad * 2);

						int xOffset = 0;
						int centerY = 256 / 2;
						if (ori.major() != Race.MIXED) {
							rect.translate(reversed ? -rect.width : 0, centerY - rect.height / 2);

							int maxCd = switch (hand.getOrigins().major()) {
								case SPIRIT -> 3;
								case DIVINITY -> 4;
								default -> 1;
							};

							Polygon clip = Graph.makePoly(new Dimension(rect.width, rect.height),
									0.5, 0,
									1, 1 / 4d,
									1, 1 / 4d * 3,
									0.5, 1,
									0, 1 / 4d * 3,
									0, 1 / 4d
							);
							clip.translate(rect.x, rect.y);
							g1.setClip(clip);

							g1.drawImage(ori.major().getBadge(), rect.x, rect.y, rect.width, rect.height, null);

							if (hand.getOriginCooldown() > 0) {
								g1.setColor(new Color(255, 0, 0, 200));

								Point offset = new Point((int) (rect.width * 1.25), (int) (rect.height * 1.25));
								g1.fillArc(
										rect.x - offset.x / 2, rect.y - offset.y / 2,
										rect.width + offset.x, rect.height + offset.y, 90,
										hand.getOriginCooldown() * 360 / maxCd
								);
							}

							g1.setClip(null);

							xOffset = rect.width + MARGIN.x;
						}

						Race[] minor = ori.minor();
						for (int i = 0; i < minor.length; i++) {
							Race r = minor[i];

							rect = new Rectangle(rad, rad);
							rect.translate(xOffset + (rad + MARGIN.x) * (i / 2), centerY - rad - MARGIN.y / 2 + (rad + MARGIN.y) * (i % 2));

							if (reversed) {
								rect.translate(-rect.x * 2 - rect.width, 0);
							}

							g1.drawImage(r.getBadge(), rect.x, rect.y, rect.width, rect.height, null);
						}
					}
			);

			Graph.applyTransformed(g, reversed ? 265 : 2240, reversed ? 1176 : 426,
					g1 -> {
						HashBag<Class<?>> count = new HashBag<>();
						count.addAll(hand.getGraveyard().stream().map(Drawable::getClass).toList());

						g1.setColor(Color.WHITE);
						g1.setFont(Fonts.UBUNTU_MONO.deriveBold(rad - 5));
						String text = "S: %2s\nE: %2s\nF: %2s\nD: %2s".formatted(
								count.getCount(Senshi.class),
								count.getCount(Evogear.class),
								count.getCount(Field.class),
								hand.getDiscard().size()
						);

						if (reversed) {
							Graph.drawMultilineString(g1, text, 0, rad - 5, 375, -10,
									(str, px, py) -> Graph.drawOutlinedString(g1, str.replace("_", " "), px, py, 6, Color.BLACK)
							);
						} else {
							Graph.drawMultilineString(g1, text, -g1.getFontMetrics().stringWidth("S: 000"), rad - 5, 375, -10,
									(str, px, py) -> Graph.drawOutlinedString(g1, str.replace("_", " "), px, py, 6, Color.BLACK)
							);
						}
					}
			);
		};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Arena arena = (Arena) o;
		return Objects.equals(slots, arena.slots) && Objects.equals(banned, arena.banned);
	}

	@Override
	public int hashCode() {
		return Objects.hash(slots, banned);
	}
}
