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

import java.util.Arrays;

public enum DonationBundle {
	SIMPLE("vzFl1enz7D", 1000, 0, true),
	SUPPORTER("NGQPKYjbo", 7500, 0, false),
	SUPPORTER_P("yxDWN85zUH", 15000, 1, false),
	SUPPORTER_PP("P3fE4ClNHr", 150000, 5, false);

	private final String id;
	private final int credits;
	private final int gems;
	private final boolean cumulative;

	DonationBundle(String id, int credits, int gems, boolean cumulative) {
		this.id = id;
		this.credits = credits;
		this.gems = gems;
		this.cumulative = cumulative;
	}

	public static DonationBundle getById(String id) {
		return Arrays.stream(values()).filter(db -> db.id.equals(id)).findFirst().orElse(null);
	}

	public String getId() {
		return id;
	}

	public int getCredits() {
		return credits;
	}

	public int getGems() {
		return gems;
	}

	public boolean isCumulative() {
		return cumulative;
	}
}
