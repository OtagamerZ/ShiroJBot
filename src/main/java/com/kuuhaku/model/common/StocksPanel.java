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

import com.kuuhaku.controller.postgresql.StockMarketDAO;
import com.kuuhaku.model.enums.Fonts;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StocksPanel {
	public final int WIDTH = 260;
	public final int HEIGHT = 110;
	private final List<StockValue> values = new ArrayList<>();

	public StocksPanel() {
		List<StockValue> values = StockMarketDAO.getValues().values().stream()
				.sorted(Comparator
						.<StockValue>comparingDouble(sv -> Math.floor(sv.getGrowth() * 1000) / 1000)
						.thenComparing(StockValue::getId)
				).collect(Collectors.toList());

		List<StockValue> high = values.stream().filter(sv -> sv.getGrowth() > 0).limit(20).collect(Collectors.toList());
		List<StockValue> low = values.stream().filter(sv -> sv.getGrowth() < 0).limit(20).collect(Collectors.toList());
		List<StockValue> stale = values.stream().filter(sv -> sv.getGrowth() == 0).limit(60 - high.size() + low.size()).collect(Collectors.toList());

		this.values.addAll(high);
		this.values.addAll(stale);
		this.values.addAll(low);
	}

	public BufferedImage view() {
		DecimalFormat df = new DecimalFormat("0.000'%'");
		BufferedImage panel = new BufferedImage(WIDTH * 6, HEIGHT * 10, BufferedImage.TYPE_INT_RGB);

		for (int i = 0; i < values.size(); i++) {
			StockValue sv = values.get(i);
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
			Profile.printCenteredString(sv.getName(), width, posX, posY + height / 3 - 9, g2d);

			g2d.setFont(Fonts.DJB_GET_DIGITAL.deriveFont(Font.PLAIN, 50));
			double growth = Math.floor(sv.getGrowth() * 1000) / 1000;
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
			Profile.printCenteredString(df.format(growth), width - 30, posX + 30, posY + height - 15, g2d);

			g2d.dispose();
		}

		return panel;
	}
}
