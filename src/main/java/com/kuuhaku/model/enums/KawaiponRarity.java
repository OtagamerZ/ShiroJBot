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

import java.util.Arrays;

public enum KawaiponRarity {
	FIELD(-3, ""),
	EQUIPMENT(-2, ""),
	FUSION(-1, ""),
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

	public static KawaiponRarity[] validValues() {
		return Arrays.stream(values()).filter(kr -> kr.index > 0).toArray(KawaiponRarity[]::new);
	}

	@Override
	public String toString() {
		return switch (this) {
			case COMMON -> "Comum";
			case UNCOMMON -> "Incomum";
			case RARE -> "Rara";
			case ULTRA_RARE -> "Ultra Rara";
			case LEGENDARY -> "Lendária";
			case ULTIMATE -> "Ultimate";
			case EQUIPMENT -> "Equipamento";
			case FIELD -> "Arena";
			default -> throw new InvalidStateException();
		};
	}

	public static KawaiponRarity getByName(String name) {
		return switch (StringUtils.stripAccents(name.toLowerCase())) {
			case "comum" -> COMMON;
			case "incomum" -> UNCOMMON;
			case "rara" -> RARE;
			case "ultra", "ultra_rara" -> ULTRA_RARE;
			case "lendaria" -> LEGENDARY;
			default -> null;
		};
	}
}
