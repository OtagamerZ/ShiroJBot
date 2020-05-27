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

package com.kuuhaku.handlers.games.tabletop.entity;

public class Spot {
	private final int x;
	private final int y;

	private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

	public Spot(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static Spot of(int x, int y) {
		return new Spot(x, y);
	}

	public static Spot of(String coord) {
		return new Spot(alphabet.indexOf(coord.toLowerCase().charAt(0)), Integer.parseInt(coord.substring(1, 2)) - 1);
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
