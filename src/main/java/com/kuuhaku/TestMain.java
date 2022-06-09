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
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMain {
	public static void main(String[] args) {
		final int zoom = 2;

		StopWatch time = new StopWatch();

		I18N locale = I18N.EN;
		Senshi card = DAO.find(Senshi.class, "NAJENDA");
		Deck deck = new Deck();
		BufferedImage bi = new Arena().render();

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
	}
}
