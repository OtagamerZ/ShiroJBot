/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.enums;

import com.kuuhaku.util.Graph;

import java.util.function.BiFunction;

public enum PixelOp {
	SUM((a, b) -> {
		int[] aRgb = Graph.unpackRGB(a);
		int[] bRgb = Graph.unpackRGB(b);
		int[] out = new int[4];

		for (int i = 0; i < out.length; i++) {
			out[i] = (aRgb[i] + bRgb[i]) / 0xFF;
		}

		return out[0] << 24 | out[1] << 16 | out[2] << 8 | out[3];
	}),
	SUBTRACT((a, b) -> {
		int[] aRgb = Graph.unpackRGB(a);
		int[] bRgb = Graph.unpackRGB(b);
		int[] out = new int[4];

		for (int i = 0; i < out.length; i++) {
			out[i] = (aRgb[i] - bRgb[i]) / 0xFF;
		}

		return out[0] << 24 | out[1] << 16 | out[2] << 8 | out[3];
	}),
	MULTIPLY((a, b) -> {
		int[] aRgb = Graph.unpackRGB(a);
		int[] bRgb = Graph.unpackRGB(b);
		int[] out = new int[4];

		for (int i = 0; i < out.length; i++) {
			out[i] = (aRgb[i] * bRgb[i]) / 0xFF;
		}

		return out[0] << 24 | out[1] << 16 | out[2] << 8 | out[3];
	}),
	DIVIDE((a, b) -> {
		int[] aRgb = Graph.unpackRGB(a);
		int[] bRgb = Graph.unpackRGB(b);
		int[] out = new int[4];

		for (int i = 0; i < out.length; i++) {
			out[i] = bRgb[i] == 0 ? 0 : (aRgb[i] / bRgb[i]) / 0xFF;
		}

		return out[0] << 24 | out[1] << 16 | out[2] << 8 | out[3];
	});

	private final BiFunction<Integer, Integer, Integer> op;

	PixelOp(BiFunction<Integer, Integer, Integer> op) {
		this.op = op;
	}

	public int get(int a, int b) {
		return op.apply(a, b);
	}
}
