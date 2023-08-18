/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku;

import java.util.Map;

public class VariableValue extends Value {
	private static final Map<String, String> EMOJIS = Map.ofEntries(
			Map.entry("php", "<:hp:1142148435454197771>"),
			Map.entry("bhp", "<:base_hp:1142150678962241577>"),
			Map.entry("pmp", "<:mp:1142148440302825585>"),
			Map.entry("pdg", "<:degen:1142148428227416116>"),
			Map.entry("prg", "<:regen:1142148442068623430>"),
			Map.entry("mp", "<:mana:1142148437408747690>"),
			Map.entry("hp", "<:blood:1142148421902405662>"),
			Map.entry("atk", "<:attack:1142148417506770954>"),
			Map.entry("dfs", "<:defense:1142148426402902030>"),
			Map.entry("ddg", "<:dodge:1142148430844678296>"),
			Map.entry("blk", "<:block:1142148419364864161> ")
	);

	private String name;

	public VariableValue(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return EMOJIS.getOrDefault(name.substring(1), name);
	}
}
