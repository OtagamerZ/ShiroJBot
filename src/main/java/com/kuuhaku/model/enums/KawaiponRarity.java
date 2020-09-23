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

import com.github.ygimenez.exception.InvalidStateException;
import org.apache.commons.lang3.StringUtils;

public enum KawaiponRarity {
	EQUIPMENT(0, ""),
	COMMON(1, "<:common:726171819664736268> "),
	UNCOMMON(2, "<:uncommon:726171819400232962> "),
	RARE(3, "<:rare:726171819853480007> "),
	ULTRA_RARE(4, "<:ultra_rare:726171819786240091> "),
	LEGENDARY(5, "<:legendary:726171819945623682> "),
	ULTIMATE(10, "");

	private final int index;
	private final String emote;

	KawaiponRarity(int index, String emote) {
		this.index = index;
		this.emote = emote;
	}

	public int getIndex() {
		return index;
	}

	public String getEmote() {
		return emote;
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
				return "Lendária";
			case ULTIMATE:
				return "Ultimate";
			default:
				throw new InvalidStateException();
		}
	}

	public static KawaiponRarity getByName(String name) {
		switch (StringUtils.stripAccents(name.toLowerCase())) {
			case "comum":
				return COMMON;
			case "incomum":
				return UNCOMMON;
			case "rara":
				return RARE;
			case "ultra_rara":
				return ULTRA_RARE;
			case "lendaria":
				return LEGENDARY;
			default:
				return null;
		}
	}
}
