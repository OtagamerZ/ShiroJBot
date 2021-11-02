/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

public enum BountyDifficulty {
	NONE(0),
	VERY_EASY(2),
	EASY(4),
	MEDIUM(6),
	HARD(8),
	VERY_HARD(10),
	IMPOSSIBLE(Integer.MAX_VALUE);

	private final int difficulty;

	BountyDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public static BountyDifficulty valueOf(int diff) {
		BountyDifficulty out = null;

		for (BountyDifficulty bd : values()) {
			if (diff >= bd.difficulty) out = bd;
			else break;
		}

		return out;
	}

	public static BountyDifficulty valueOf(double diff) {
		return valueOf((int) Math.round(diff));
	}

	@Override
	public String toString() {
		return switch (this) {
			case NONE -> "Nenhuma";
			case VERY_EASY -> "Muito fácil";
			case EASY -> "Fácil";
			case MEDIUM -> "Médio";
			case HARD -> "Difícil";
			case VERY_HARD -> "Muito difícil";
			case IMPOSSIBLE -> "Impossível";
		};
	}
}
