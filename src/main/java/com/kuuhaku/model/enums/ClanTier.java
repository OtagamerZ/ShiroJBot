/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.utils.Helper;

public enum ClanTier {
	PARTY("Grupo", 10, 50_000, 10_000),
	FACTION("Facção", 50, 250_000, 35_000),
	GUILD("Guilda", 100, 1_500_000, 150_000),
	DYNASTY("Dinastia", 500, 0, 1_000_000);

	private final String name;
	private final int capacity;
	private final int vaultSize;
	private final long cost;

	ClanTier(String name, int capacity, int vaultSize, long cost) {
		this.name = name;
		this.capacity = capacity;
		this.vaultSize = vaultSize;
		this.cost = cost;
	}

	public String getName() {
		return name;
	}

	public int getCapacity() {
		return capacity;
	}

	public int getVaultSize() {
		return vaultSize;
	}

	public long getCost() {
		return cost;
	}

	public long getRent() {
		return cost / 4;
	}

	public ClanTier getNext() {
		return Helper.getNext(this, values());
	}
}
