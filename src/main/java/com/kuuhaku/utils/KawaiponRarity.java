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

package com.kuuhaku.utils;

import com.github.ygimenez.exception.InvalidStateException;

public enum KawaiponRarity {
	COMMON(5),
	UNCOMMON(4),
	RARE(3),
	ULTRA_RARE(2),
	LEGENDARY(1);

	private final int index;

	KawaiponRarity(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		switch (this) {
			case COMMON:
				return "Comum";
			case UNCOMMON:
				return "Incomum";
			case RARE:
				return "Rara";
			case ULTRA_RARE:
				return "Ultra Rara";
			case LEGENDARY:
				return "Legendária";
			default:
				throw new InvalidStateException();
		}
	}
}
