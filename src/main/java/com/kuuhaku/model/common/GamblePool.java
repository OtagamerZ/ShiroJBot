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

import java.util.ArrayList;
import java.util.List;

public class GamblePool {
	public static class Gamble {
		private final String slot;
		private final int weight;

		public Gamble(String s, int w) {
			this.slot = s;
			this.weight = w;
		}

		String getSlot() {
			return slot;
		}

		int getWeight() {
			return weight;
		}
	}

	public void addGamble(Gamble gamble) {
		for (int i = 0; i < gamble.getWeight(); i++) {
			g.add(gamble);
		}
	}

	private static final List<Gamble> g = new ArrayList<>();

	public String[] getPool() {
		List<String> pool = new ArrayList<>();
		for (Gamble gamble : g) {
			pool.add(gamble.getSlot());
		}

		return pool.toArray(new String[0]);
	}
}
