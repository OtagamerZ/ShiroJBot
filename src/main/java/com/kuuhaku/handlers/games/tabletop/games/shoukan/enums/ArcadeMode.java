package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import com.kuuhaku.utils.helpers.LogicHelper;

import java.util.Arrays;

public enum ArcadeMode {
	NONE,
	ROULETTE("roleta"),
	BLACKROCK,
	INSTAKILL,
	CARDMASTER;

	private final String[] aliases;

	ArcadeMode(String... aliases) {
		this.aliases = aliases;
	}

	public static ArcadeMode get(String name) {
		return Arrays.stream(values())
				.filter(a -> a.name().equalsIgnoreCase(name) || LogicHelper.containsAny(name, a.aliases))
				.findFirst()
				.orElse(NONE);
	}
}
