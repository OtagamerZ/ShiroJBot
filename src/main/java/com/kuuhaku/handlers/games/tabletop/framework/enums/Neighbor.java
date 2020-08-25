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

package com.kuuhaku.handlers.games.tabletop.framework.enums;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum Neighbor {
	UPPER_LEFT(new int[]{-1, -1}),
	UP(new int[]{0, -1}),
	UPPER_RIGHT(new int[]{1, -1}),
	LEFT(new int[]{-1, 0}),
	CENTER(new int[]{0, 0}),
	RIGHT(new int[]{1, 0}),
	LOWER_LEFT(new int[]{-1, 1}),
	DOWN(new int[]{0, 1}),
	LOWER_RIGHT(new int[]{1, 1}),

	L_LEFT_UP(new int[]{-2, -1}),
	L_LEFT_DOWN(new int[]{-2, 1}),
	L_UP_LEFT(new int[]{-1, -2}),
	L_UP_RIGHT(new int[]{1, -2}),
	L_RIGHT_UP(new int[]{2, -1}),
	L_RIGHT_DOWN(new int[]{2, 1}),
	L_DOWN_LEFT(new int[]{-1, 2}),
	L_DOWN_RIGHT(new int[]{1, 2}),

	LARGE_CASTLING(new int[]{-2, 0}),
	SHORT_CASTLING(new int[]{2, 0});

	private final int[] coord;

	Neighbor(int[] coord) {
		this.coord = coord;
	}

	public int[] getCoord() {
		return coord;
	}

	public int getX() {
		return coord[0];
	}

	public int getY() {
		return coord[1];
	}

	public static Neighbor getByVector(int[] vector) {
		for (Neighbor n : Neighbor.values()) {
			if (Arrays.equals(n.coord, vector)) return n;
		}
		throw new NoSuchElementException();
	}

	public static Neighbor[] getCommonMoves() {
		return Arrays.stream(Neighbor.values()).filter(n -> !n.name().startsWith("L_") && !n.name().contains("CASTLING")).toArray(Neighbor[]::new);
	}

	public static Neighbor[] getKnightMoves() {
		return Arrays.stream(Neighbor.values()).filter(n -> n.name().startsWith("L_") && !n.name().contains("CASTLING")).toArray(Neighbor[]::new);
	}

	public static Neighbor[] getCastlingMoves() {
		return Arrays.stream(Neighbor.values()).filter(n -> n.name().contains("CASTLING")).toArray(Neighbor[]::new);
	}
}
