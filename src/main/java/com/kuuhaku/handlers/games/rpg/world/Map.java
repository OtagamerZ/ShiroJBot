/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.games.rpg.world;

import com.kuuhaku.handlers.games.rpg.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class Map {
	private final String map;
	private int[] size;
	private final int[] defaultPos;
	private final static String alpha = "abcdefghijklmnopqrstuvwxyz";

	public Map(String url, int[] defaultPos) throws IOException, IllegalArgumentException {
		this.map = initMap(url);
		this.defaultPos = defaultPos;
	}

	private String initMap(String url) throws IOException, IllegalArgumentException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedImage map = ImageIO.read(con.getInputStream());

		if (map.getWidth() % 64 != 0 || map.getHeight() % 64 != 0) {
			BufferedImage rescaledMap = new BufferedImage(64 * Math.round(map.getWidth() / 64f), 64 * Math.round(map.getHeight() / 64f), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = rescaledMap.createGraphics();

			g2d.drawImage(map.getScaledInstance(64 * Math.round(map.getWidth() / 64f), 64 * Math.round(map.getHeight() / 64f), 0), 0, 0, null);
			g2d.dispose();
			map = rescaledMap;
		}

		this.size = new int[]{map.getWidth() / 64, map.getHeight() / 64};
		if (size[0] > 26 || size[1] > 26) throw new IllegalArgumentException();

		BufferedImage overlay = new BufferedImage(map.getWidth() + 64, map.getHeight() + 64, BufferedImage.TYPE_INT_RGB);
		BufferedImage pointer = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("coords.png")));
		Graphics2D g2d = overlay.createGraphics();
		g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		g2d.setFont(new Font("Arial", Font.PLAIN, 40));

		g2d.drawImage(pointer, 0, 0, null);
		g2d.drawImage(map, 64, 64, null);
		for (int x = 1; x <= size[0]; x++) {
			g2d.setColor(Color.BLACK);
			g2d.drawLine(64 * (x + 1), 0, 64 * (x + 1), overlay.getHeight());
			g2d.setColor(Color.WHITE);
			String str = String.valueOf(alpha.charAt(x - 1)).toUpperCase();
			g2d.drawString(str, (64 * x) + ((64 / 2f) - Math.round(g2d.getFontMetrics().getStringBounds(str, g2d).getWidth() / 2)), 64 - Math.round(g2d.getFontMetrics().getStringBounds(str, g2d).getHeight() / 2));
		}

		for (int y = 1; y <= size[1]; y++) {
			g2d.setColor(Color.BLACK);
			g2d.drawLine(0, 64 * (y + 1), overlay.getWidth(), 64 * (y + 1));
			g2d.setColor(Color.WHITE);
			g2d.drawString(String.valueOf(alpha.charAt(y - 1)), (64 / 2f) - Math.round(g2d.getFontMetrics().getStringBounds(String.valueOf(alpha.charAt(y - 1)), g2d).getWidth() / 2), (64 * (y + 1)) - Math.round(g2d.getFontMetrics().getStringBounds(String.valueOf(alpha.charAt(y - 1)), g2d).getHeight() / 2));
		}

		g2d.dispose();

		return Utils.encodeToBase64(overlay);
	}

	public BufferedImage getMap() {
		return Utils.decodeBase64(map);
	}

	public int[] getDefaultPos() {
		return defaultPos;
	}

	public int[] getSize() {
		return size;
	}
}
