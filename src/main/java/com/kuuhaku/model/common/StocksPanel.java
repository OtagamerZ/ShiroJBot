/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import com.kuuhaku.tabletop.utils.Helper;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;

public class StocksPanel {
	private final int WIDTH = 260;
	private final int HEIGHT = 110;
	private final List<String> val = FileUtils.readLines(new File(this.getClass().getClassLoader().getResource("values.txt").toURI()), StandardCharsets.UTF_8);

	public StocksPanel() throws URISyntaxException, IOException {
	}

	public BufferedImage view() {
		DecimalFormat df = new DecimalFormat("0.000'%'");
		BufferedImage panel = new BufferedImage(WIDTH * 6, HEIGHT * 10, BufferedImage.TYPE_INT_RGB);

		for (int i = 0; i < 60; i++) {
			Graphics2D g2d = panel.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int posX = WIDTH * (i / 10) + 5;
			int posY = HEIGHT * (i % 10) + 5;
			int width = WIDTH - 10;
			int height = HEIGHT - 10;

			g2d.setColor(new Color(17, 16, 16));
			g2d.fillRect(WIDTH * (i / 10), HEIGHT * (i % 10), WIDTH, HEIGHT);

			g2d.setColor(new Color(12, 9, 9));
			g2d.fillRect(posX, posY, width, height);

			g2d.setColor(new Color(29, 28, 28));
			g2d.fillRect(posX, posY, width, height / 3);

			g2d.setColor(Color.white);
			g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 18));
			Helper.printCenteredString(val.get(i), width, posX, posY + height / 3 - 9, g2d);

			g2d.setFont(Fonts.DJB_GET_DIGITAL.deriveFont(Font.PLAIN, 50));
			double growth = Math.random() * 20 - 10;
			df.setPositivePrefix(growth == 0 ? "" : "+");
			if (growth > 0) {
				g2d.setColor(Color.GREEN);
				g2d.fillPolygon(
						new int[]{
								posX + 10,
								posX + 40,
								posX + 25
						}, new int[]{
								posY + height / 3 + 50,
								posY + height / 3 + 50,
								posY + height / 3 + 20
						},
						3
				);
			} else if (growth < 0) {
				g2d.setColor(Color.RED);
				g2d.fillPolygon(
						new int[]{
								posX + 10,
								posX + 40,
								posX + 25
						}, new int[]{
								posY + height / 3 + 20,
								posY + height / 3 + 20,
								posY + height / 3 + 50
						},
						3
				);
			} else {
				g2d.setColor(Color.ORANGE);
			}
			Helper.printCenteredString(df.format(growth), width - 30, posX + 30, posY + height - 15, g2d);

			g2d.dispose();
		}

		return panel;
	}
}
