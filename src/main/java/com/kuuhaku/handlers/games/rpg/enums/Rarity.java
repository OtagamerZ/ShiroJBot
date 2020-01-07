package com.kuuhaku.handlers.games.rpg.enums;

import java.util.Random;

public enum Rarity {
	COMMON(0.55f, "Comum"), UNCOMMON(0.25f, "Incomum"), RARE(0.12f, "Raro"), EPIC(0.07f, "Épico"), LEGENDARY(0.01f, "Lendário");

	private final float weight;
	private final String name;

	Rarity(float weight, String name) {
		this.weight = weight;
		this.name = name;
	}

	public float getWeight() {
		return weight;
	}

	public static Rarity byName(String name) throws IllegalArgumentException {
		switch (name.toLowerCase()) {
			case "comum":
				return Rarity.COMMON;
			case "incomum":
				return Rarity.UNCOMMON;
			case "raro":
				return Rarity.RARE;
			case "epico":
			case "épico":
				return Rarity.EPIC;
			case "lendario":
			case "lendário":
				return Rarity.LEGENDARY;
				default: throw new IllegalArgumentException();
		}
	}

	public String getName() {
		return name;
	}

	public static Rarity roll(int luck) {
		float rng = new Random().nextFloat();
		if (rng <= LEGENDARY.weight * (1 + luck / 100f)) {
			return LEGENDARY;
		} else if (rng <= EPIC.weight * (1 + luck / 80f)) {
			return EPIC;
		} else if (rng <= RARE.weight * (1 + luck / 60f)) {
			return RARE;
		} else if (rng <= UNCOMMON.weight * (1 + luck / 40f)) {
			return UNCOMMON;
		} else {
			return COMMON;
		}
	}
}
