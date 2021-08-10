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

public enum DonationBundle {
	SUPPORTER(7500, 0),
	SUPPORTER_P(15000, 1),
	SUPPORTER_PP(150000, 5);

	private final int credits;
	private final int gems;

	DonationBundle(int credits, int gems) {
		this.credits = credits;
		this.gems = gems;
	}

	public static DonationBundle getByValue(int value) {
		if (value < 10) return SUPPORTER;
		else if (value < 25) return SUPPORTER_P;
		else return SUPPORTER_PP;
	}

	public int getCredits() {
		return credits;
	}

	public int getGems() {
		return gems;
	}
}
