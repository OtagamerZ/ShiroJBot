/*
 * This file is part of Shiro J Bot.
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
	CHILD(1, 0),
	TEEN(0.5f, 5000),
	ADULT(0, 25000);

	private final float trainability;
	private final int requiredXp;

	Tier(float trainability, int requiredXp) {
		this.trainability = trainability;
		this.requiredXp = requiredXp;
	}

	public float getTrainability() {
		return trainability;
	}

	public int getRequiredXp() {
		return requiredXp;
	}

	public int xpToNext(int xp) {
		switch (this) {
			case CHILD:
				return Tier.TEEN.requiredXp - xp;
			case TEEN:
				return Tier.ADULT.requiredXp - xp;
			default:
				return 0;
		}
	}

	public Tier next() {
		switch (this) {
			case CHILD:
				return Tier.TEEN;
			case TEEN:
				return Tier.ADULT;
			default:
				return null;
		}
	}

	@Override
	public String toString() {
		switch (this) {
			case CHILD:
				return "Filhote";
			case TEEN:
				return "Adolescente";
			case ADULT:
				return "Adulto";
			default:
				throw new RuntimeException();
		}
	}
}
