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
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.IO;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMain {
	public static void main(String[] args) {
		testDeck();
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
