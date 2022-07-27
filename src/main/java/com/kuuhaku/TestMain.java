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
		Dimension SIZE = new Dimension(950, 600);

		BufferedImage mask = IO.getResourceAsImage("assets/profile_mask.png");
		BufferedImage overlay = IO.getResourceAsImage("assets/profile_overlay.png");

		String bio = "Test test test test";
		Color color = new Color(0xB256C7);

		JFrame frame = new JFrame("Test");
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHints(Constants.SD_HINTS);

				BufferedImage bg = IO.getImage("https://i.imgur.com/XMp6nLY.jpeg");
				Graph.applyMask(bg, mask, 0);
				g2d.drawImage(bg, 0, 0, null);
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
