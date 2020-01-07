package com.kuuhaku.model;

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
		g.add(gamble);
	}

	private static final List<Gamble> g = new ArrayList<>();

	public String[] getPool() {
		List<String> pool = new ArrayList<>();
		for (Gamble gamble : g) {
			for (int x = gamble.getWeight(); x > 0; x--) {
				pool.add(gamble.getSlot());
			}
		}

		return pool.toArray(new String[0]);
	}
}
