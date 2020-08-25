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

package com.kuuhaku.handlers.games.tabletop.framework;

import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;

public class Spot {
	private final int x;
	private final int y;
	private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private Spot(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static Spot of(int x, int y) {
		return new Spot(x, y);
	}

	public static Spot of(String coord) {
		int x = alphabet.indexOf(String.valueOf(coord.charAt(0)).toUpperCase());
		return new Spot(x, Integer.parseInt(String.valueOf(coord.charAt(1))) - 1);
	}

	public Spot getNeighbor(Neighbor ngh) {
		return new Spot(x + ngh.getX(), y + ngh.getY());
	}

	public int[] getCoords() {
		return new int[]{x, y};
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public static String getAlphabet() {
		return alphabet;
	}
}
