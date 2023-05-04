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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.util.Graph;

import java.awt.*;
import java.util.Arrays;

public enum Rarity {
	COMMON(1, 0xFFFFFF, "<:common:726171819664736268> "),
	UNCOMMON(2, 0x03BB85, "<:uncommon:726171819400232962> "),
	RARE(3, 0x70D1F4, "<:rare:726171819853480007> "),
	EPIC(4, 0x9966CC, "<:epic:1103772898407223348> "),
	LEGENDARY(5, 0xDC9018, "<:legendary:726171819945623682> "),
	ULTIMATE(-1, 0xD400AA, "<:ultimate:1002748864643743774> "),
	EVOGEAR,
	FIELD("\uD83C\uDFD4️ "),
	FUSION,
	NONE;

	private final int index;
	private final int color;
	private final String emote;

	Rarity(int index, int color, String emote) {
		this.index = index;
		this.color = color;
		this.emote = emote;
	}

	Rarity(String emote) {
		this.index = -1;
		this.color = 0;
		this.emote = emote;
	}

	Rarity() {
		this.index = -1;
		this.color = 0;
		this.emote = "";
	}

	public int getIndex() {
		return index;
	}

	public Color getColor(boolean chrome) {
		Color color = new Color(this.color);
		if (chrome) {
			color = Graph.rotate(color, 180);
		}

		return color;
	}

	public String getEmote(Card card) {
		if (card != null && this == EVOGEAR) {
			int tier = card.asEvogear().getTier();
			return switch (tier) {
				case 1 -> "<:tier_1:1087709790899294260> ";
				case 2 -> "<:tier_2:1087709792291782666> ";
				case 3 -> "<:tier_3:1087709793407488021> ";
				case 4 -> "<:tier_4:1087709788865048626> ";
				default -> "\uD83E\uDDE7 ";
			};
		}

		return emote;
	}

	public int getCount() {
		return DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM card WHERE rarity = ?1", name());
	}

	public static Rarity[] getActualRarities() {
		return Arrays.stream(values())
				.filter(r -> r.getIndex() > 0)
				.toArray(Rarity[]::new);
	}

	public static Rarity fromIndex(int i) {
		return Arrays.stream(values())
				.filter(r -> r.getIndex() == i)
				.findFirst().orElse(null);
	}
}
