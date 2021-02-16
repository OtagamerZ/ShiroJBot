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

package com.kuuhaku.handlers.games.tabletop.utils;

import java.math.RoundingMode;

public class Helper {

	public static double[] normalize(double[] vector) {
		double length = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
		return new double[]{vector[0] / length, vector[1] / length};
	}

	public static float[] normalize(float[] vector) {
		double length = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
		return new float[]{(float) (vector[0] / length), (float) (vector[1] / length)};
	}

	public static int[] normalize(int[] vector, RoundingMode roundingMode) {
		double length = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
		return switch (roundingMode) {
			case UP, CEILING, HALF_UP -> new int[]{mirroredCeil(vector[0] / length), mirroredCeil(vector[1] / length)};
			case DOWN, FLOOR, HALF_DOWN -> new int[]{mirroredFloor(vector[0] / length), mirroredFloor(vector[1] / length)};
			case HALF_EVEN -> new int[]{(int) Math.round(vector[0] / length), (int) Math.round(vector[1] / length)};
			default -> throw new IllegalArgumentException();
		};
	}

	public static int mirroredCeil(double value) {
		return (int) (value < 0 ? Math.floor(value) : Math.ceil(value));
	}

	public static int mirroredFloor(double value) {
		return (int) (value > 0 ? Math.floor(value) : Math.ceil(value));
	}
}
