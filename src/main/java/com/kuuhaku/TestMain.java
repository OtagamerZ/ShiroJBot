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

package com.kuuhaku;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.shoukan.Arena;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMain {
	public static void main(String[] args) {
		Point MARGIN = new Point(25, 25);
		Dimension SIZE = new Dimension(
				(225 + MARGIN.x * 2) * 5 /* slots */ + (225 + MARGIN.x * 2) * 4 /* side stacks */,
				(350 + MARGIN.y) * 4 /* slots */ + MARGIN.y * 10 - 1
		);
		Dimension BAR_SIZE = new Dimension(SIZE.width / 2, 100);

		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			int mana = 34;
			int hp = 5100;
			int mHp = 5000;
			int regdeg = 1500;
			int cd = 2;
			int mCd = 3;
			Origin ori = new Origin(Race.HUMAN, Race.BEAST);
			Map<Lock, Integer> locks = Map.of(
					Lock.DECK, 2,
					Lock.EFFECT, 5
			);

			@Override
			public void paint(Graphics g) {
				super.paint(g);

				boolean reverse = true;
				Graphics2D g2d = (Graphics2D) g;
				if (reverse) {
					g2d.scale(-1, -1);
					g2d.translate(-BAR_SIZE.width, -BAR_SIZE.height);
				}

				double h = 1 - 2d / BAR_SIZE.height;
				double w = 1 - 2d / BAR_SIZE.width;
				Polygon boundaries = Graph.makePoly(BAR_SIZE,
						1 - w, h / 3d,
						w / 30d, 1 - h,
						w / 1.5, 1 - h,
						w / 1.4, h / 3d,
						w / 1.1, h / 3d,
						w, h,
						1 - w, h
				);
				g2d.setColor(Color.ORANGE);
				g2d.fill(boundaries);

				g2d.setClip(boundaries);

				int mpWidth = (int) (BAR_SIZE.width / 2.25);
				Color manaOver1 = new Color(0x1181FF);
				Color manaOver2 = new Color(0x4D15FF);
				for (int i = 0; i < 33; i++) {
					g2d.setColor(Color.CYAN);
					if (mana > 66 + i) {
						g2d.setColor(manaOver2);
					} else if (mana > 33 + i) {
						g2d.setColor(manaOver1);
					} else if (mana <= i) {
						g2d.setColor(Color.DARK_GRAY);
					}

					g2d.fillRect(
							(int) (BAR_SIZE.width / 1.4 - (mpWidth / 33 - 0.75) * 33) + (mpWidth / 33 - 1) * i + 2,
							2, mpWidth / 33 - 5, BAR_SIZE.height / 3 - 2
					);
				}

				double barWidth = BAR_SIZE.width - (BAR_SIZE.width / 1.5 - (mpWidth / 33d - 0.75) * 33);
				Rectangle2D.Double bar = new Rectangle2D.Double(
						BAR_SIZE.width - barWidth * 1.025, BAR_SIZE.height / 3d + 4 - (reverse ? 2 : 0),
						barWidth * 1.025, BAR_SIZE.height / 1.75
				);
				g2d.setColor(Color.DARK_GRAY);
				g2d.fill(bar);

				double fac = (double) hp / mHp;
				if (fac >= 2 / 3d) {
					g2d.setColor(new Color(69, 173, 28));
				} else if (fac >= 1 / 3d) {
					g2d.setColor(new Color(197, 158, 0));
				} else {
					g2d.setColor(new Color(173, 28, 28));
				}
				g2d.fill(new Rectangle2D.Double(bar.x, bar.y, bar.width * Math.min(fac, 1), bar.height));

				if (fac > 1) {
					g2d.setColor(new Color(0, 255, 149));
					g2d.fill(new Rectangle2D.Double(bar.x, bar.y, bar.width * Calc.clamp(fac - 1, 0, 1), bar.height));
				}

				if (regdeg != 0) {
					BufferedImage tex = IO.getResourceAsImage("shoukan/" + (regdeg > 0 ? "r" : "d") + "egen_overlay.png");
					g2d.setPaint(new TexturePaint(
							tex,
							new Rectangle2D.Double(bar.x, bar.y + bar.height + (reverse ? 1 : 0), bar.height, bar.height * (reverse ? -1 : 1) + (reverse ? -1 : 1))
					));
					g2d.fill(new Rectangle2D.Double(bar.x, bar.y, bar.width * Math.min((double) Math.abs(regdeg) / mHp, 1), bar.height));
				}

				int radius = BAR_SIZE.height - 10;
				List<BufferedImage> icons = ori.images();
				for (int i = 0; i < 2; i++) {
					int slotX = (int) ((BAR_SIZE.width - barWidth * 1.025) / 2 - (radius + 15)) + (radius + 15) * i;

					g2d.setColor(new Color(127, 127, 127, 150));
					Polygon poly = Graph.makePoly(new Dimension(radius, radius),
							0.5, 0,
							1, 1 / 4d,
							1, 1 / 4d * 3,
							0.5, 1,
							0, 1 / 4d * 3,
							0, 1 / 4d
					);
					poly.translate(15 + slotX, 5);
					g2d.setClip(poly);
					g2d.fill(poly);

					if (reverse) {
						g2d.drawImage(icons.get(i),
								10 + slotX + radius, radius,
								-(radius - 10), -(radius - 10),
								null
						);
					} else {
						g2d.drawImage(icons.get(i),
								20 + slotX, 10,
								radius - 10, radius - 10,
								null
						);
					}

					g2d.setColor(new Color(255, 0, 0, 200));
					g2d.fillArc(
							15 + slotX - radius / 2, 5 - radius / 2,
							radius * 2, radius * 2, 90 * (reverse ? -1 : 1),
							cd * 360 / mCd
					);
				}
				g2d.setClip(null);

				Graph.applyTransformed(g2d, reverse ? -1 : 1, g1 -> {
					String mpText = "MP: " + StringUtils.leftPad(String.valueOf(mana), 2, "0");
					g1.setColor(Color.CYAN);
					g1.setFont(new Font("Arial", Font.BOLD, BAR_SIZE.height / 3 - 2));

					if (reverse) {
						Graph.drawOutlinedString(g1, mpText,
								(int) -(bar.x + g1.getFontMetrics().stringWidth(mpText)), (int) -(bar.y - BAR_SIZE.height / 3 + 4),
								6, Color.BLACK
						);
					} else {
						Graph.drawOutlinedString(g1, mpText,
								(int) bar.x, (int) (bar.y - 6),
								6, Color.BLACK
						);
					}

					String hpText = "HP: "
							+ StringUtils.leftPad(String.valueOf(hp), 4, "0")
							+ "/"
							+ StringUtils.leftPad(String.valueOf(mHp), 4, "0");
					g1.setColor(Color.WHITE);
					g1.setFont(new Font("Arial", Font.BOLD, (int) (BAR_SIZE.height / 2.5)));

					if (reverse) {
						String rdText = "";
						if (regdeg != 0) {
							rdText = (regdeg > 0 ? " +" : " ") + regdeg;
							g1.setColor(regdeg < 0 ? new Color(0xAD0000) : new Color(0x009DFF));
							Graph.drawOutlinedString(g1, rdText,
									(int) -(bar.x + 6 + g1.getFontMetrics().stringWidth(rdText)), (int) -(bar.y + 6),
									6, Color.BLACK
							);
						}

						g1.setColor(Color.WHITE);
						Graph.drawOutlinedString(g1, hpText,
								(int) -(bar.x + 6 + g1.getFontMetrics().stringWidth(hpText + rdText)), (int) -(bar.y + 6),
								6, Color.BLACK
						);
					} else {
						if (regdeg != 0) {
							String rdText = (regdeg > 0 ? " +" : " ") + regdeg;
							g1.setColor(regdeg < 0 ? new Color(0xAD0000) : new Color(0x009DFF));
							Graph.drawOutlinedString(g1, rdText,
									(int) (bar.x + 6 + g1.getFontMetrics().stringWidth(hpText)), (int) (bar.y + bar.height - 6),
									6, Color.BLACK
							);
						}

						g1.setColor(Color.WHITE);
						Graph.drawOutlinedString(g1, hpText,
								(int) (bar.x + 6), (int) (bar.y + bar.height - 6),
								6, Color.BLACK
						);
					}
				});

				g2d.setClip(null);
				g2d.setColor(Color.ORANGE);
				g2d.setStroke(new BasicStroke(5));
				g2d.draw(boundaries);
				g2d.setStroke(new BasicStroke());

				Graph.applyTransformed(g2d, reverse ? -1 : 1, g1 -> {
					Lock[] values = Lock.values();
					for (int i = 0; i < values.length; i++) {
						Lock lock = values[i];
						boolean locked = locks.containsKey(lock);

						int rad = BAR_SIZE.height / 3 - 4;
						int x = (int) (BAR_SIZE.width / 1.4) + (BAR_SIZE.height / 3 + 35) * i;
						if (reverse) {
							g1.drawImage(lock.getImage(locked),
									-(x + rad - 5) - rad, -rad,
									rad, rad,
									null
							);
						} else {
							g1.drawImage(lock.getImage(locked),
									x, 0,
									rad, rad,
									null
							);
						}

						if (locked) {
							g1.setColor(Color.RED);
							g1.setFont(new Font("Arial", Font.BOLD, rad - 5));
							String text = String.valueOf(locks.get(lock));

							if (reverse) {
								Graph.drawOutlinedString(g1, text,
										-(x + rad - g1.getFontMetrics().stringWidth(text)), (int) -(bar.y - rad),
										6, Color.BLACK
								);
							} else {
								Graph.drawOutlinedString(g1, text,
										x + rad + 5, (int) (bar.y - 14),
										6, Color.BLACK
								);
							}
						}
					}
				});
			}
		};

		panel.setSize(BAR_SIZE);

		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setSize(panel.getSize());
		frame.setVisible(true);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(frame::repaint, 0, 1, TimeUnit.SECONDS);
	}

	public static void testDeck() {
		final int zoom = 2;

		StopWatch time = new StopWatch();

		I18N locale = I18N.EN;
		Deck deck = DAO.find(Account.class, "350836145921327115").getCurrentDeck();

		BufferedImage bi = deck.render(locale);

		AtomicInteger renders = new AtomicInteger();
		JLabel fps = new JLabel("0");

		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(bi, 10, 10, bi.getWidth() / zoom, bi.getHeight() / zoom, null);

				if (time.isStarted()) {
					g.setColor(Color.RED);
					fps.setText(String.valueOf(renders.incrementAndGet() / (1 + time.getTime() / 1000)));
					repaint();
				}
			}
		};

		panel.add(fps);
		panel.setSize(bi.getWidth() / zoom + 20, bi.getHeight() / zoom + 20);

		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setSize(panel.getSize());
		frame.setVisible(true);

		time.start();

		try {
			File f = new File("field.webp");
			if (!f.exists()) f.createNewFile();

			Files.write(f.toPath(), IO.getBytes(bi, "webp", 1));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void testField() {
		final int zoom = 2;

		StopWatch time = new StopWatch();

		I18N locale = I18N.EN;
		Senshi card = DAO.find(Senshi.class, "NAJENDA");
		Deck deck = new Deck();
		Arena arena = new Arena(null);
		BufferedImage bi = arena.render(locale);

		AtomicInteger renders = new AtomicInteger();
		JLabel fps = new JLabel("0");

		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(bi, 10, 10, bi.getWidth() / zoom, bi.getHeight() / zoom, null);

				if (time.isStarted()) {
					g.setColor(Color.RED);
					fps.setText(String.valueOf(renders.incrementAndGet() / (1 + time.getTime() / 1000)));
					repaint();
				}
			}
		};

		panel.add(fps);
		panel.setSize(bi.getWidth() / zoom + 20, bi.getHeight() / zoom + 20);

		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setSize(panel.getSize());
		frame.setVisible(true);

		time.start();

		try {
			Files.write(Path.of("C:\\Users\\DEV202\\Desktop\\field.webp"), IO.getBytes(bi, "webp"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
