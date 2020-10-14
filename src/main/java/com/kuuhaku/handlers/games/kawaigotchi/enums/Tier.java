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

package com.kuuhaku.handlers.games.kawaigotchi.enums;

public enum Tier {
	CHILD(1, 0, 1),
	TEEN(0.5f, 5000, 1.25f),
	ADULT(0, 15000, 1.5f);

	private final float trainability;
	private final int requiredXp;
	private final float userXpMult;

	Tier(float trainability, int requiredXp, float userXpMult) {
		this.trainability = trainability;
		this.requiredXp = requiredXp;
		this.userXpMult = userXpMult;
	}

	public float getTrainability() {
		return trainability;
	}

	public int getRequiredXp() {
		return requiredXp;
	}

	public float getUserXpMult() {
		return userXpMult;
	}

	public Tier next() {
		if (this == Tier.CHILD) {
			return TEEN;
		}
		return ADULT;
	}

	public static Tier tierByXp(float xp) {
		if (xp >= ADULT.getRequiredXp()) return ADULT;
		else if (xp >= TEEN.getRequiredXp() && xp < ADULT.getRequiredXp()) return TEEN;
		else return CHILD;
	}

	@Override
	public String toString() {
		return switch (this) {
			case CHILD -> "Filhote";
			case TEEN -> "Adolescente";
			case ADULT -> "Adulto";
		};
	}
}
