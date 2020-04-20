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

public enum Stance {
	IDLE(true, true, true, false),
	SLEEPING(false, false, false, true),
	HAPPY(true, true, true, false),
	SAD(true, false, false, false),
	ANGRY(true, false, true, false),
	DEAD(false, false, false, false);

	private final boolean canEat;
	private final boolean canPlay;
	private final boolean canTrain;
	private final boolean resting;

	Stance(boolean canEat, boolean canPlay, boolean canTrain, boolean resting) {
		this.canEat = canEat;
		this.canPlay = canPlay;
		this.canTrain = canTrain;
		this.resting = resting;
	}

	public boolean canEat() {
		return canEat;
	}

	public boolean canPlay() {
		return canPlay;
	}

	public boolean canTrain() {
		return canTrain;
	}

	public boolean isResting() {
		return resting;
	}

	@Override
	public String toString() {
		switch (this) {
			case IDLE:
				return "Ocioso";
			case SLEEPING:
				return "Dormindo";
			case HAPPY:
				return "Feliz";
			case SAD:
				return "Triste";
			case ANGRY:
				return "Bravo";
			case DEAD:
				return "Morto";
			default:
				return null;
		}
	}
}
