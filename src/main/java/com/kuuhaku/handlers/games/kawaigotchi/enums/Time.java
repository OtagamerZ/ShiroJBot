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

package com.kuuhaku.handlers.games.kawaigotchi.enums;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

public enum Time {
	DAY(new Image[]{
			new ImageIcon(Objects.requireNonNull(Time.class.getClassLoader().getResource("sky_day.jpg"))).getImage(),
			new ImageIcon(Objects.requireNonNull(Time.class.getClassLoader().getResource("gb_no_overlay.png"))).getImage()
	}, 9, 18),
	//18 -> 22 lerp
	NIGHT(new Image[]{
			new ImageIcon(Objects.requireNonNull(Time.class.getClassLoader().getResource("sky_night.jpg"))).getImage(),
			new ImageIcon(Objects.requireNonNull(Time.class.getClassLoader().getResource("gb_overlay.png"))).getImage()
	}, 22, 29);
	//5 -> 9 lerp

	private final Image[] parallax;
	private final int start;
	private final int end;

	Time(Image[] parallax, int start, int end) {
		this.parallax = parallax;
		this.start = start;
		this.end = end;
	}

	public static Image[] getParallax() {
		int time = OffsetDateTime.now(ZoneId.of("GMT-3")).getHour();

		if (inRange(DAY, time)) {
			return DAY.parallax;
		} else if (inRange(NIGHT, time)) {
			return NIGHT.parallax;
		} else if (time >= 18 && time < 22) {
			return new Image[]{
					interpolate(DAY.parallax[0], NIGHT.parallax[0], (time - 18) / (float) 4),
					interpolate(DAY.parallax[1], NIGHT.parallax[1], (time - 18) / (float) 4)
			};
		} else if (time >= 5 && time < 9) {
			return new Image[]{
					interpolate(NIGHT.parallax[0], DAY.parallax[0], (time - 5) / (float) 4),
					interpolate(NIGHT.parallax[1], DAY.parallax[1], (time - 5) / (float) 4)
			};
		} else throw new RuntimeException();
	}

	public static boolean inRange(Time time, int value) {
		int x = (value >= 0 && value < 5 ? value + 24 : value);
		return x >= time.start && x < time.end;
	}

	private static Image interpolate(Image from, Image to, float fac) {
		BufferedImage bi = new BufferedImage(from.getWidth(null), from.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(from, 0, 0, null);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fac));
		g2d.drawImage(to, 0, 0, null);
		g2d.dispose();

		return bi;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}
}
