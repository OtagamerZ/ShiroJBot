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

import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TestMain {
	public static void main(String[] args) {
		Dimension SIZE = new Dimension(400, 100);

		BufferedImage cd = IO.getImage("https://api.shirojbot.site/v2/card/AKAME_GA_KILL/CHELSEA.png");
		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHints(Constants.HD_HINTS);

				g2d.setColor(Color.black);
				g2d.drawImage(cd, 0, 0, cd.getWidth() / 2, cd.getHeight() / 2, null);
				g2d.fill(Graph.makePoly(SIZE,
						0.35, 0,
						1, 0,
						1, 1,
						0.3, 1
				));
			}
		};

		panel.setSize(SIZE);
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setSize(panel.getSize());
		frame.setVisible(true);
	}

	/*
	public static void main(String[] args) {
		Dimension SIZE = new Dimension(950, 600);

		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHints(Constants.SD_HINTS);


			}
		};

		panel.setSize(SIZE);
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setSize(panel.getSize());
		frame.setVisible(true);
	}
	 */
}
