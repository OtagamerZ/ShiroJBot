/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.games.Shoukan;
import com.kuuhaku.games.engine.Renderer;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.BondedLinkedList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.IO;
import com.kuuhaku.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class Arena implements Renderer {
	private final Field DEFAULT = DAO.find(Field.class, "DEFAULT");
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
	private final List<Drawable<?>> banned = new BondedLinkedList<>(Drawable::reset);
	private Field field = null;

	public Arena(Shoukan game) {
		this.game = game;
		slots = Map.of(
				Side.TOP, Utils.generate(5, i -> new SlotColumn(Side.TOP, i)),
				Side.BOTTOM, Utils.generate(5, i -> new SlotColumn(Side.BOTTOM, i))
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
		return slots.get(side).stream().allMatch(sc -> sc.getTop() == null);
	}

	public List<Drawable<?>> getBanned() {
		return banned;
	}

	public Field getField() {
		return Utils.getOr(field, DEFAULT);
	}

	public void setField(Field field) {
		this.field = field;
	}

	@Override
	public BufferedImage render(I18N locale) {
		Hand top = game.getHands().get(Side.TOP);
		Hand bottom = game.getHands().get(Side.BOTTOM);

		BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height + BAR_SIZE.height * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		Graph.applyTransformed(g2d, drawBar(top));

		Graph.applyTransformed(g2d, 0, BAR_SIZE.height + 1, g1 -> {
			g1.drawImage(getField().renderBackground(), 0, 0, null);

			for (Side side : Side.values()) {
				int xOffset = CENTER.x - ((225 + MARGIN.x) * 5 - MARGIN.x) / 2;
				int yOffset = switch (side) {
					case TOP -> CENTER.y - (350 + MARGIN.y) * 2 - MARGIN.y * 4;
					case BOTTOM -> CENTER.y + MARGIN.y * 5;
				};

				Deck deck = game.getHands().get(side).getUserDeck();
				Graph.applyTransformed(g1, xOffset, yOffset, g2 -> {
					for (SlotColumn slot : slots.get(side)) {
						int x = (225 + MARGIN.x) * slot.getIndex();
						int equips, frontline, backline;

						if (side == Side.TOP) {
							equips = 350 * 2 + MARGIN.y + MARGIN.y / 4;
							frontline = 350 + MARGIN.y;
							backline = 0;
						} else {
							equips = -350 / 3 - MARGIN.y / 4;
							frontline = 0;
							backline = 350 + MARGIN.y;
						}

						if (slot.hasTop()) {
							Senshi s = slot.getTop();

							g2.drawImage(s.render(locale, deck), x, frontline, null);
							if (!s.getEquipments().isEmpty()) {
								Graph.applyTransformed(g2, x, equips, g3 -> {
									Dimension resized = new Dimension(225 / 3, 350 / 3);
									int middle = 225 / 2 - resized.width / 2;

									for (int i = 0; i < s.getEquipments().size(); i++) {
										g3.drawImage(s.getEquipments().get(i).render(locale, deck),
												middle + (resized.width + MARGIN.x / 2) * (i - 1), 0,
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
				});
			}

			Graph.applyTransformed(g1, MARGIN.x, 0, g2 -> {
				if (!top.getDeck().isEmpty()) {
					Deck d = top.getUserDeck();
					g2.drawImage(d.getFrame().getBack(d),
							0, CENTER.y - 350 / 2 - (350 + MARGIN.y), null
					);
				}
				if (!banned.isEmpty()) {
					Drawable<?> d = banned.get(0);
					g2.drawImage(d.render(locale, d.getHand().getUserDeck()),
							0, CENTER.y - 350 / 2, null
					);
				}
				if (!bottom.getGraveyard().isEmpty()) {
					Drawable<?> d = bottom.getGraveyard().get(0);
					g2.drawImage(d.render(locale, bottom.getUserDeck()),
							0, CENTER.y - 350 / 2 + (350 + MARGIN.y), null
					);
				}
			});

			Graph.applyTransformed(g1, SIZE.width - 225 - MARGIN.x, 0, g2 -> {
				if (!top.getGraveyard().isEmpty()) {
					Drawable<?> d = top.getGraveyard().get(0);
					g2.drawImage(d.render(locale, top.getUserDeck()),
							0, CENTER.y - 350 / 2 - (350 + MARGIN.y), null
					);
				}
				if (!getField().getId().equals("DEFAULT")) {
					g2.drawImage(getField().render(locale, Utils.getOr(() -> getField().getHand().getUserDeck(), Deck.INSTANCE)),
							0, CENTER.y - 350 / 2, null
					);
				}
				if (!bottom.getDeck().isEmpty()) {
					Deck d = bottom.getUserDeck();
					g2.drawImage(d.getFrame().getBack(d),
							0, CENTER.y - 350 / 2 + (350 + MARGIN.y), null
					);
				}
			});
		});

		Graph.applyTransformed(g2d, drawBar(bottom));

		return bi;
	}

	private Consumer<Graphics2D> drawBar(Hand hand) {
		return g -> {
			boolean reversed = hand.getSide() != Side.TOP;

			BufferedImage bi = new BufferedImage(BAR_SIZE.width + BAR_SIZE.height * 2, BAR_SIZE.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHints(Constants.HD_HINTS);

			Point2D padPrcnt = new Point2D.Double(0.01, 0.1);
			Point padUnit = new Point((int) (BAR_SIZE.width * padPrcnt.getX()), (int) (BAR_SIZE.height * padPrcnt.getY()));

			Graph.applyTransformed(g2d, BAR_SIZE.height * 2, 0, g1 -> {
				g1.setColor(hand.getUserDeck().getFrame().getThemeColor());
				Polygon boundaries = Graph.makePoly(BAR_SIZE,
						0, 1,
						1, 1,
						0.95, 0.5,
						0.7, 0.5,
						0.65, 0,
						0, 0
				);
				g1.fill(boundaries);

				double ratio = ((1 - padPrcnt.getY()) - (0.5 + padPrcnt.getY())) / (1 - 0.5);

				boundaries = Graph.makePoly(BAR_SIZE,
						0 + padPrcnt.getX(), 1 - padPrcnt.getY(),
						0.95 - padPrcnt.getX() + 0.05 * ratio, 1 - padPrcnt.getY(),
						0.95 - padPrcnt.getX(), 0.5 + padPrcnt.getY(),
						0.7 - padPrcnt.getX(), 0.5 + padPrcnt.getY(),
						0.65 - padPrcnt.getX(), 0 + padPrcnt.getY(),
						0 + padPrcnt.getX(), 0 + padPrcnt.getY()
				);
				g1.setClip(boundaries);

				Rectangle bar = new Rectangle(
						boundaries.getBounds().x, BAR_SIZE.height / 2 + padUnit.y,
						boundaries.getBounds().width, BAR_SIZE.height / 2 - padUnit.y * 2
				);
				g1.setColor(Color.DARK_GRAY);
				g1.fill(bar);

				double fac = hand.getHPPrcnt();
				if (fac >= 2 / 3d) {
					g1.setColor(new Color(69, 173, 28));
				} else if (fac >= 1 / 3d) {
					g1.setColor(new Color(173, 161, 28));
				} else {
					g1.setColor(new Color(173, 28, 28));
				}

				bar.setSize((int) (boundaries.getBounds().width * Math.min(fac, 1)), bar.height);
				g1.fill(bar);
				if (fac > 1) {
					g1.setColor(new Color(0, 255, 149));
					bar.setSize((int) (boundaries.getBounds().width * Math.min(fac - 1, 1)), bar.height);
					g1.fill(bar);
				}

				if (hand.getRegen() != 0) {
					fac = hand.getRegenPrcnt();
					bar.setSize((int) (boundaries.getBounds().width * Math.min(Math.abs(fac), 1)), bar.height);
					g1.setPaint(new TexturePaint(
							IO.getResourceAsImage("shoukan/" + (fac > 0 ? "regen" : "degen") + "_overlay.png"),
							new Rectangle2D.Double(0, 0, bar.height, bar.height)
					));
					g1.fill(bar);
					g1.setPaint(null);
				}

				Color manaOver1 = new Color(46, 95, 255);
				Color manaOver2 = new Color(77, 21, 255);
				g1.setStroke(new BasicStroke(14));
				for (int i = 0; i < 33; i++) {
					g1.setColor(Color.CYAN);
					if (hand.getMP() > 66 + i) {
						g1.setColor(manaOver2);
					} else if (hand.getMP() > 33 + i) {
						g1.setColor(manaOver1);
					} else if (hand.getMP() <= i) {
						g1.setColor(Color.DARK_GRAY);
					}

					int x = (int) (boundaries.getBounds().x + boundaries.getBounds().width * (0.695 - padPrcnt.getX()) - boundaries.getBounds().width * 0.016 * (32 - i));

					g1.drawLine(
							x, boundaries.getBounds().y,
							x, boundaries.getBounds().y + (int) (boundaries.getBounds().height * (0.5 - padPrcnt.getY()))
					);
				}

				g1.setClip(null);

				Point pos = new Point(padUnit.x + 4, BAR_SIZE.height / 2 - 3);
				if (reversed) {
					Graph.applyTransformed(g1, -1, g2 -> {
						drawValuesInverted(g2, hand, pos.x, (int) (pos.y + padUnit.y * 1.6), BAR_SIZE.height / 3 + 10 + padUnit.y);

						int rad = bi.getHeight() / 2 - padUnit.y;
						g2.setColor(Color.RED);
						g2.setFont(new Font("Arial", Font.BOLD, rad / 3 * 2));
						for (int i = 0; i < Lock.values().length; i++) {
							Lock lock = Lock.values()[i];
							int time = hand.getLockTime(lock);

							Graph.applyTransformed(g2, (int) -(BAR_SIZE.width * 0.65 + padUnit.x + (rad + padUnit.x * 4) * (Lock.values().length - i)), -(rad + padUnit.y / 2), g3 -> {
								g3.drawImage(lock.getImage(time > 0), 0, 0, rad, rad, null);
								if (time > 0) {
									Graph.drawOutlinedString(g3, String.valueOf(time), rad + padUnit.x / 2, rad / 2 * 2, 6, Color.BLACK);
								}
							});
						}
					});
				} else {
					drawValues(g1, hand, pos.x, pos.y, BAR_SIZE.height / 3 - 3 + padUnit.y);

					int rad = bi.getHeight() / 2 - padUnit.y;
					g1.setColor(Color.RED);
					g1.setFont(new Font("Arial", Font.BOLD, rad / 3 * 2));
					for (int i = 0; i < Lock.values().length; i++) {
						Lock lock = Lock.values()[i];
						int time = hand.getLockTime(lock);

						Graph.applyTransformed(g1, (int) (SIZE.width * 0.35 + (rad + padUnit.x * 4) * i), padUnit.y / 2, g2 -> {
							g2.drawImage(lock.getImage(time > 0), 0, 0, rad, rad, null);
							if (time > 0) {
								Graph.drawOutlinedString(g2, String.valueOf(time), rad + padUnit.x / 2, rad / 2 * 2, 6, Color.black);
							}
						});
					}
				}
			});

			g2d.setColor(hand.getUserDeck().getFrame().getThemeColor());
			g2d.fillPolygon(Graph.makePoly(
					BAR_SIZE.height / 3, 0,
					BAR_SIZE.height * 2, 0,
					BAR_SIZE.height * 2, BAR_SIZE.height,
					0, BAR_SIZE.height,
					0, BAR_SIZE.height / 3
			));

			int rad = BAR_SIZE.height - padUnit.y * 2;
			List<BufferedImage> images = hand.getOrigin().images();
			if (reversed) {
				for (int i = 0; i < 2; i++) {
					g2d.drawImage(images.get(i),
							padUnit.x * 2 + (rad + padUnit.x) * i + rad, padUnit.y + rad,
							-rad, -rad, null
					);
				}
			} else {
				for (int i = 0; i < 2; i++) {
					g2d.drawImage(images.get(i),
							padUnit.x * 2 + (rad + padUnit.x) * i, padUnit.y,
							rad, rad, null
					);
				}
			}

			g2d.dispose();

			int x;
			int y;
			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 3 * 2));

			if (reversed) {
				g.drawImage(bi,
						SIZE.width, BAR_SIZE.height * 2 + SIZE.height,
						-bi.getWidth(), -bi.getHeight(),
						null
				);

				x = MARGIN.x;
				y = SIZE.height + BAR_SIZE.height + (BAR_SIZE.height + BAR_SIZE.height / 3) / 2 + 10;
			} else {
				g.drawImage(bi, 0, 0, null);

				x = SIZE.width - MARGIN.x - g.getFontMetrics().stringWidth(hand.getName());
				y = (BAR_SIZE.height + BAR_SIZE.height / 3) / 2 + 10;
			}

			Graph.drawOutlinedString(g, hand.getName(), x, y, 10, Color.black);
		};
	}

	private void drawValues(Graphics2D g2d, Hand hand, int x, int y, int spacing) {
		g2d.setColor(Color.CYAN);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 2));
		String mpText = "MP: " + StringUtils.leftPad(String.valueOf(hand.getMP()), 2, "0");
		Graph.drawOutlinedString(g2d, mpText, x, y, 6, Color.BLACK);

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 4));
		String hpText = "HP: " + StringUtils.leftPad(String.valueOf(hand.getHP()), 4, "0") + " / " + StringUtils.leftPad(String.valueOf(hand.getBase().hp()), 4, "0");
		Graph.drawOutlinedString(g2d, hpText, x, y + spacing, 6, new Color(0, 0, 0, 200));

		if (hand.getRegen() != 0) {
			boolean degen = hand.getRegen() < 0;

			g2d.setColor(degen ? Color.RED : new Color(0x009DFF));
			g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 4));
			String regText = (degen ? "" : "+") + StringUtils.leftPad(String.valueOf(hand.getRegen()), 4, "0");

			Graph.drawOutlinedString(g2d, regText,
					x + g2d.getFontMetrics().stringWidth(hpText) + MARGIN.x / 2, y + spacing,
					6, new Color(0, 0, 0, 200)
			);
		}
	}

	private void drawValuesInverted(Graphics2D g2d, Hand hand, int x, int y, int spacing) {
		x = -x;
		y = -y;

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 4));
		String hpText = "HP: " + StringUtils.leftPad(String.valueOf(hand.getHP()), 4, "0") + " / " + StringUtils.leftPad(String.valueOf(hand.getBase().hp()), 4, "0");

		int offset = g2d.getFontMetrics().stringWidth(hpText);
		Graph.drawOutlinedString(g2d, hpText, x - offset, y, 6, new Color(0, 0, 0, 200));

		if (hand.getRegen() != 0) {
			boolean degen = hand.getRegen() < 0;

			g2d.setColor(degen ? Color.RED : new Color(0x009DFF));
			g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 4));
			String regText = (degen ? "" : "+") + StringUtils.leftPad(String.valueOf(hand.getRegen()), 4, "0");

			Graph.drawOutlinedString(g2d, regText,
					x - offset + g2d.getFontMetrics().stringWidth(hpText) + MARGIN.x / 2, y,
					6, new Color(0, 0, 0, 200)
			);
		}

		g2d.setColor(Color.CYAN);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 2));
		String mpText = "MP: " + StringUtils.leftPad(String.valueOf(hand.getMP()), 2, "0");

		offset = g2d.getFontMetrics().stringWidth(mpText);
		Graph.drawOutlinedString(g2d, mpText, x - offset, y + spacing, 6, Color.BLACK);
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
