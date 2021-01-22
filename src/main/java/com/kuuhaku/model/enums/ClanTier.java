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

public enum ClanTier {
	PARTY("Grupo", 10, 100000, 50000),
	FACTION("Facção", 50, 500000, 125000),
	GUILD("Guilda", 100, 2000000, 1000000),
	DYNASTY("Dinastia", 500, 0, 0);

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
}
