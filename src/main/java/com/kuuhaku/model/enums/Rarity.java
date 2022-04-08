/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

public enum Rarity {
	COMMON(1),
	UNCOMMON(2),
	RARE(3),
	ULTRA_RARE(4),
	LEGENDARY(5),
	ULTIMATE(-1),
	EVOGEAR(-1),
	FIELD(-1),
	FUSION(-1);

	private final int index;

	Rarity(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return switch (this) {
			case COMMON -> "Comum";
			case UNCOMMON -> "Incomum";
			case RARE -> "Rara";
			case ULTRA_RARE -> "Ultra rara";
			case LEGENDARY -> "Lendária";
			case ULTIMATE -> "Ultimate";
			case EVOGEAR -> "Evogear";
			case FIELD -> "Campo";
			case FUSION -> "Fusão";
		};
	}
}
