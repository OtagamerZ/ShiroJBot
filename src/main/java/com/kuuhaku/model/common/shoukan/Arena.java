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
import com.kuuhaku.interfaces.Drawable;
import com.kuuhaku.model.common.BondedLinkedList;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Arena {
	private final Point MARGIN = new Point(25, 25);
	private final Dimension SIZE = new Dimension(
			(225 + MARGIN.x * 2) * 5 /* slots */ + (225 + MARGIN.x * 2) * 2 /* side stacks */,
			(350 + MARGIN.y) * 4 /* slots */ + MARGIN.y * 10
	);
	private final Point CENTER = new Point(SIZE.width / 2, SIZE.height / 2);
	private final Dimension BAR_SIZE = new Dimension(SIZE.width / 2, 100);
	/*
          ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐
          │  │ │  │ │  │ │  │ │  │
    ┌──┐  └──┘ └──┘ └──┘ └──┘ └──┘  ┌──┐
    │  │  ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐  │  │
    └──┘  │  │ │  │ │  │ │  │ │  │  └──┘
	┌──┐  └──┘ └──┘ └──┘ └──┘ └──┘  ┌──┐
	│  │  ---------- 50 ----------  │  │
	└──┘  ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐  └──┘
	┌──┐  │  │ │  │ │  │ │  │ │  │  ┌──┐
    │  │  └──┘ └──┘ └──┘ └──┘ └──┘  │  │
    └──┘  ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐  └──┘
          │  │ │  │ │  │ │  │ │  │
          └──┘ └──┘ └──┘ └──┘ └──┘
	*/

	private final Map<Side, List<SlotColumn>> slots;
	private final List<Drawable> banned = new BondedLinkedList<>(Drawable::reset);

	public Arena() {
		slots = Map.of(
				Side.TOP, Utils.generate(5, i -> new SlotColumn(Side.TOP, i)),
				Side.BOTTOM, Utils.generate(5, i -> new SlotColumn(Side.BOTTOM, i))
		);
	}

	public Map<Side, List<SlotColumn>> getSlots() {
		return slots;
	}

	public List<SlotColumn> getSlots(Side side) {
		return slots.get(side);
	}

	public BufferedImage render() {
		BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height + BAR_SIZE.height * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Graph.applyTransformed(g2d, 0, 0, drawBar(new Hand("350836145921327115", Side.TOP, new Origin(Race.HUMAN, Race.MACHINE), new BaseValues())));

		/*
		Graph.applyTransformed(g2d, 0, BAR_SIZE.height, g1 -> {
			g1.setColor(Color.BLACK);
			g1.fillRect(0, 0, SIZE.width, SIZE.height);
			g1.setColor(Color.WHITE);

			for (Side side : Side.values()) {
				int xOffset = CENTER.x - ((225 + MARGIN.x) * 5 - MARGIN.x) / 2;
				int yOffset = switch (side) {
					case TOP -> CENTER.y - (350 + MARGIN.y) * 2 - MARGIN.y * 4;
					case BOTTOM -> CENTER.y + MARGIN.y * 5;
				};

				Graph.applyTransformed(g1, xOffset, yOffset, g -> {
					for (SlotColumn slot : slots.get(side)) {
						int x = (225 + MARGIN.x) * slot.getIndex();
						int y = switch (side) {
							case TOP -> (350 + MARGIN.y) * 2;
							case BOTTOM -> -350 / 4 - MARGIN.y;
						};

						Graph.applyTransformed(g, x, y, g2 -> {
							Dimension resized = new Dimension(225 / 4, 350 / 4);
							int middle = 225 / 2 - resized.width / 2;

							for (int i = 0; i < 3; i++) {
								g2.fillRect(middle + (resized.width + MARGIN.x / 2) * (i - 1), 0, resized.width, resized.height);
							}
						});

						g.fillRect(x, 0, 225, 350);
						g.fillRect(x, 350 + MARGIN.y, 225, 350);
					}
				});
			}

			Graph.applyTransformed(g1, MARGIN.x, 0, g2 -> {
				g2.fillRect(0, CENTER.y - 350 / 2 - (350 + MARGIN.y), 225, 350);
				g2.fillRect(0, CENTER.y - 350 / 2, 225, 350);
				g2.fillRect(0, CENTER.y - 350 / 2 + (350 + MARGIN.y), 225, 350);
			});

			Graph.applyTransformed(g1, SIZE.width - 225 - MARGIN.x, 0, g2 -> {
				g2.fillRect(0, CENTER.y - 350 / 2 - (350 + MARGIN.y), 225, 350);
				g2.fillRect(0, CENTER.y - 350 / 2, 225, 350);
				g2.fillRect(0, CENTER.y - 350 / 2 + (350 + MARGIN.y), 225, 350);
			});

			g1.setColor(Color.RED);
			g1.setStroke(new BasicStroke(2));
			g1.drawLine(0, CENTER.y, SIZE.width, CENTER.y);
			g1.drawLine(CENTER.x, 0, CENTER.x, SIZE.height);
			g1.drawRect(0, 0, SIZE.width - 1, SIZE.height - 1);
		});
		*/

		Graph.applyTransformed(g2d, 0, 0, drawBar(new Hand("572413282653306901", Side.BOTTOM, new Origin(Race.MYSTICAL, Race.DEMON), new BaseValues())));

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
				g1.setColor(Color.BLACK);
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
				g1.setColor(new Color(0, 0, 0, 150));
				g1.fill(boundaries);
				g1.setClip(boundaries);

				Rectangle bar = new Rectangle(
						boundaries.getBounds().x, BAR_SIZE.height / 2 + padUnit.y,
						boundaries.getBounds().width, BAR_SIZE.height / 2 - padUnit.y * 2
				);
				g1.setColor(Color.DARK_GRAY);
				g1.fill(bar);

				hand.setHp(3500);
				double fac = hand.getHpPrcnt();
				if (fac >= 2 / 3d) {
					g1.setColor(new Color(69, 173, 28));
				} else if (fac >= 1 / 3d) {
					g1.setColor(new Color(173, 161, 28));
				} else {
					g1.setColor(new Color(173, 28, 28));
				}

				bar.translate((int) (-bar.width * (1 - Math.min(fac, 1))), 0);
				g1.fill(bar);
				if (fac > 1) {
					g1.setColor(new Color(0, 255, 149));
					bar.translate((int) (-bar.width * (1 - (fac - 1))), 0);
					g1.fill(bar);
				}

				Color manaOver1 = new Color(46, 95, 255);
				Color manaOver2 = new Color(77, 21, 255);
				g1.setStroke(new BasicStroke(10));
				for (int i = 0; i < 33; i++) {
					g1.setColor(Color.CYAN);
					if (hand.getMp() > 66 + i) {
						g1.setColor(manaOver2);
					} else if (hand.getMp() > 33 + i) {
						g1.setColor(manaOver1);
					} else if (hand.getMp() <= i) {
						g1.setColor(Color.DARK_GRAY);
					}

					int x = (int) (boundaries.getBounds().x + boundaries.getBounds().width * (0.695 - padPrcnt.getX()) - boundaries.getBounds().width * 0.015 * (32 - i));

					g1.drawLine(
							x, boundaries.getBounds().y,
							x, boundaries.getBounds().y + (int) (boundaries.getBounds().height * (0.5 - padPrcnt.getY()))
					);
				}

				g1.setClip(null);

				Point pos = new Point(padUnit.x + 2, BAR_SIZE.height / 2 - 3);
				if (reversed) {
					Graph.applyTransformed(g1, -1, g2 ->
							drawValuesInverted(g1, hand, pos.x, pos.y, BAR_SIZE.height / 3 + padUnit.y)
					);
				} else {
					drawValues(g1, hand, pos.x, pos.y, BAR_SIZE.height / 3 - 3 + padUnit.y);
				}
			});

			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, BAR_SIZE.height * 2, BAR_SIZE.height);

			g2d.dispose();

			if (reversed) {
				g.drawImage(bi,
						SIZE.width, BAR_SIZE.height * 2 + SIZE.height,
						-bi.getWidth(), -bi.getHeight(),
						null
				);
			} else {
				g.drawImage(bi, 0, 0, null);
			}
			/*
			g.setColor(Color.black);
			int rad = BAR_SIZE.height - vPadUnit * 2;
			List<BufferedImage> images = hand.getOrigin().images();
			g.setRenderingHints(Constants.HD_HINTS);

			if (reversed) {
				int x = SIZE.width - (hPadUnit + (rad + hPadUnit) * 2);
				g.fillRect(x, BAR_SIZE.height + SIZE.height, SIZE.width - x, BAR_SIZE.height);
				g.drawImage(bi, x, BAR_SIZE.height * 2 + SIZE.height, -BAR_SIZE.width, -BAR_SIZE.height, null);

				for (int i = 0; i < 2; i++) {
					x = SIZE.width - (hPadUnit / 2 + (rad + hPadUnit) * (i + 1));

					g.drawImage(images.get(i), x, BAR_SIZE.height + SIZE.height + vPadUnit, rad, rad, null);
				}
			} else {
				int x = hPadUnit + (rad + hPadUnit) * 2;

				g.fillRect(0, 0, x, BAR_SIZE.height);
				g.drawImage(bi, x, 0, BAR_SIZE.width, BAR_SIZE.height, null);

				for (int i = 0; i < 2; i++) {
					x = hPadUnit / 2 + (rad + hPadUnit) * i;

					g.drawImage(images.get(i), x, vPadUnit, rad, rad, null);
				}
			}

			int x;
			int y;

			g.setColor(Color.cyan);
			g.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 2));
			String mpText = "MP: " + StringUtils.leftPad(String.valueOf(hand.getMp()), 2, "0");
			if (reversed) {
				x = (int) (BAR_SIZE.width * 2 * (1 - hPad) - g.getFontMetrics().stringWidth(mpText)) - (hPadUnit + (rad + hPadUnit) * 2);
				y = SIZE.height + BAR_SIZE.height + boundaries.getBounds().y + boundaries.getBounds().height - 2;
			} else {
				x = hPadUnit * 3 + (rad + hPadUnit) * 2;
				y = boundaries.getBounds().y + boundaries.getBounds().height / 2 - 2;
			}

			Graph.drawOutlinedString(g, mpText, x, y, 6, Color.black);

			g.setColor(Color.white);
			g.setFont(new Font("Arial", Font.PLAIN, BAR_SIZE.height / 3));
			String hpText = "HP: " + StringUtils.leftPad(String.valueOf(hand.getHp()), 4, "0") + " / " + StringUtils.leftPad(String.valueOf(hand.getBase().hp()), 4, "0");
			if (reversed) {
				x = (int) (BAR_SIZE.width * 2 * (1 - hPad) - g.getFontMetrics().stringWidth(hpText)) - (hPadUnit + (rad + hPadUnit) * 2);
				y = SIZE.height + BAR_SIZE.height + (BAR_SIZE.height / 2 - vPadUnit - 4);
			} else {
				x = hPadUnit * 3 + (rad + hPadUnit) * 2;
				y = boundaries.getBounds().y + boundaries.getBounds().height - 4;
			}

			Graph.drawOutlinedString(g, hpText, x, y, 6, Color.black);

			g.setFont(new Font("Arial", Font.PLAIN, BAR_SIZE.height));
			if (reversed) {
				x = 0;
				y = (int) (SIZE.height + BAR_SIZE.height * 1.9);
			} else {
				x = SIZE.width - g.getFontMetrics().stringWidth(hand.getName());
				y = (int) (BAR_SIZE.height * 0.75);
			}

			Graph.drawOutlinedString(g, hand.getName(), x, y, 10, Color.black);
			 */
		};
	}

	private void drawValues(Graphics2D g2d, Hand hand, int x, int y, int spacing) {
		g2d.setColor(Color.CYAN);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 2));
		String mpText = "MP: " + StringUtils.leftPad(String.valueOf(hand.getMp()), 2, "0");
		Graph.drawOutlinedString(g2d, mpText, x, y, 6, Color.BLACK);

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 3));
		String hpText = "HP: " + StringUtils.leftPad(String.valueOf(hand.getHp()), 4, "0") + " / " + StringUtils.leftPad(String.valueOf(hand.getBase().hp()), 4, "0");
		Graph.drawOutlinedString(g2d, hpText, x, y + spacing, 6, Color.BLACK);
	}

	private void drawValuesInverted(Graphics2D g2d, Hand hand, int x, int y, int spacing) {
		x = -x;

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 3));
		String hpText = "HP: " + StringUtils.leftPad(String.valueOf(hand.getHp()), 4, "0") + " / " + StringUtils.leftPad(String.valueOf(hand.getBase().hp()), 4, "0");

		int offset = g2d.getFontMetrics().stringWidth(hpText);
		Graph.drawOutlinedString(g2d, hpText, x - offset, y, 6, Color.BLACK);

		g2d.setColor(Color.CYAN);
		g2d.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 2));
		String mpText = "MP: " + StringUtils.leftPad(String.valueOf(hand.getMp()), 2, "0");

		offset = g2d.getFontMetrics().stringWidth(mpText);
		Graph.drawOutlinedString(g2d, mpText, x - offset, y - spacing, 6, Color.BLACK);
	}
}
