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

import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import org.jdesktop.swingx.graphics.BlendComposite;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class TestMain {
	public static void main(String[] args) {
		Dimension SIZE = new Dimension(950, 600);

		BufferedImage mask = IO.getResourceAsImage("assets/profile_mask.webp");
		BufferedImage overlay = Graph.toColorSpace(IO.getResourceAsImage("assets/profile_overlay.webp"), BufferedImage.TYPE_INT_ARGB);
		BufferedImage hex = IO.getResourceAsImage("assets/hex_grid.webp");

		BufferedImage bg = Graph.scaleAndCenterImage(Graph.toColorSpace(IO.getImage("https://i.imgur.com/XMp6nLY.jpeg"), BufferedImage.TYPE_INT_ARGB), SIZE.width, SIZE.height);
		String bio = "Test test test test";
		//Color color = new Color(0xCECECE);
		Color color = new Color(0xC37CCB);

		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHints(Constants.HD_HINTS);

				Graph.applyMask(bg, mask, 0);
				g2d.drawImage(bg, 0, 0, null);

				Graphics2D og2d = overlay.createGraphics();
				og2d.setColor(color);
				og2d.setComposite(BlendComposite.Multiply);
				og2d.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
				og2d.dispose();

				Polygon inner = Graph.makePoly(
						6, 75,
						475, 75,
						525, 31,
						897, 31,
						944, 78,
						944, 547,
						897, 594,
						53, 594,
						6, 547
				);

				Graph.applyMask(hex, mask, 0, true);
				g2d.drawImage(hex, 0, 0, null);

				Graph.applyTransformed(g2d, g1 -> {
					Color bgCol = new Color((200 << 24) | (color.getRGB() & 0xFFFFFF), true);

					g1.setClip(inner);
					g1.setColor(bgCol);

					RoundRectangle2D wids = new RoundRectangle2D.Double(-14, 110, 200, 50, 20, 20);

					g1.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 15));
					String[] info = {"WWWWWWWWWWWWWWWWWWWWWWWWWW", "Bla", "Bla", "Bla", "Bla", "Bla", "Bla", "Bla", "Bla", "Bla"};
					for (String s : info) {
						Rectangle2D bounds = Graph.getStringBounds(g1, s);
						int y = (int) wids.getY();

						g1.setColor(bgCol);
						wids.setFrame(wids.getX(),  y, 43 + bounds.getWidth(), bounds.getHeight() * 3);
						Graph.drawOutlined(g1, wids, 1, Color.BLACK);
						wids.setFrame(wids.getX(),  y + wids.getHeight() + 10, 0, 0);

						g1.setColor(Color.WHITE);
						Graph.drawOutlinedString(g1, s, 15, (int) (y + bounds.getHeight() * 2), 3, Color.BLACK);
					}

					g1.setColor(bgCol);
					Shape desc = new RoundRectangle2D.Double(
							SIZE.width - SIZE.width / 2d - 40, SIZE.height - SIZE.height / 3d - 20,
							SIZE.width / 2d, SIZE.height / 3d,
							20, 20
					);
					Graph.drawOutlined(g1, desc, 1, Color.BLACK);
				});

				Graph.applyMask(overlay, mask, 1);
				g2d.drawImage(overlay, 0, 0, null);
			}
		};

		panel.setSize(SIZE);
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setSize(panel.getSize());
		frame.setVisible(true);
	}
}
