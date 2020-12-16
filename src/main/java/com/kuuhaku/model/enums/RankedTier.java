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

package com.kuuhaku.model.enums;

public enum RankedTier {
	UNRANKED(0, "Unranked", 10),

	MINARAI_IV(1, "Minarai IV", 3),
	MINARAI_III(1, "Minarai III", 3),
	MINARAI_II(1, "Minarai II", 3),
	MINARAI_I(1, "Minarai I", 5),

	KAISHI_IV(2, "Kaishi IV", 3),
	KAISHI_III(2, "Kaishi III", 3),
	KAISHI_II(2, "Kaishi II", 3),
	KAISHI_I(2, "Kaishi I", 5),

	DESHI_IV(3, "Deshi IV", 3),
	DESHI_III(3, "Deshi III", 3),
	DESHI_II(3, "Deshi II", 3),
	DESHI_I(3, "Deshi I", 5),

	JUKUTATSU_IV(4, "Jukutatsu IV", 3),
	JUKUTATSU_III(4, "Jukutatsu III", 3),
	JUKUTATSU_II(4, "Jukutatsu II", 3),
	JUKUTATSU_I(4, "Jukutatsu I", 5),

	SHUJIN(5, "Shujin", 5),
	JOUKYUU(6, "Joukyuu", 5),
	DAI_MAJUTSU_SHI(7, "Dai Majutsu-shi", 0);

	private final int tier;
	private final String name;
	private final int md;

	RankedTier(int tier, String name, int md) {
		this.tier = tier;
		this.name = name;
		this.md = md;
	}

	public int getTier() {
		return tier;
	}

	public String getName() {
		return name;
	}

	public int getMd() {
		return md;
	}

	public RankedTier getNext() {
		return switch (this) {
			case UNRANKED -> MINARAI_IV;
			case MINARAI_IV -> MINARAI_III;
			case MINARAI_III -> MINARAI_II;
			case MINARAI_II -> MINARAI_I;
			case MINARAI_I -> KAISHI_IV;
			case KAISHI_IV -> KAISHI_III;
			case KAISHI_III -> KAISHI_II;
			case KAISHI_II -> KAISHI_I;
			case KAISHI_I -> DESHI_IV;
			case DESHI_IV -> DESHI_III;
			case DESHI_III -> DESHI_II;
			case DESHI_II -> DESHI_I;
			case DESHI_I -> JUKUTATSU_IV;
			case JUKUTATSU_IV -> JUKUTATSU_III;
			case JUKUTATSU_III -> JUKUTATSU_II;
			case JUKUTATSU_II -> JUKUTATSU_I;
			case JUKUTATSU_I -> SHUJIN;
			case SHUJIN -> JOUKYUU;
			case JOUKYUU -> DAI_MAJUTSU_SHI;
			case DAI_MAJUTSU_SHI -> null;
		};
	}

	public RankedTier getPrevious() {
		return switch (this) {
			case UNRANKED -> null;
			case MINARAI_IV -> null;
			case MINARAI_III -> MINARAI_IV;
			case MINARAI_II -> MINARAI_III;
			case MINARAI_I -> MINARAI_II;
			case KAISHI_IV -> MINARAI_I;
			case KAISHI_III -> KAISHI_IV;
			case KAISHI_II -> KAISHI_III;
			case KAISHI_I -> KAISHI_II;
			case DESHI_IV -> KAISHI_I;
			case DESHI_III -> DESHI_IV;
			case DESHI_II -> DESHI_III;
			case DESHI_I -> DESHI_II;
			case JUKUTATSU_IV -> DESHI_I;
			case JUKUTATSU_III -> JUKUTATSU_IV;
			case JUKUTATSU_II -> JUKUTATSU_III;
			case JUKUTATSU_I -> JUKUTATSU_II;
			case SHUJIN -> JUKUTATSU_I;
			case JOUKYUU -> SHUJIN;
			case DAI_MAJUTSU_SHI -> JOUKYUU;
		};
	}
}