package com.kuuhaku.model.enums.dunhun;

public enum RarityClass {
	NORMAL, MAGIC(2), RARE(4), UNIQUE;

	private final int maxMods;

	RarityClass() {
		this(0);
	}

	RarityClass(int maxMods) {
		this.maxMods = maxMods;
	}

	public int getMaxMods() {
		return maxMods;
	}
}
