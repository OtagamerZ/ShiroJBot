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
			Map.entry("php", "<:hp:1142172799264366703>"),
			Map.entry("bhp", "<:base_hp:1142172785049862237>"),
			Map.entry("pmp", "<:mp:1142172839412252742>"),
			Map.entry("pdg", "<:degen:1142172793811775578>"),
			Map.entry("prg", "<:regen:1142172841668780092>"),
			Map.entry("mp", "<:mana:1142172802594635827>"),
			Map.entry("hp", "<:blood:1142172788703113257>"),
			Map.entry("atk", "<:attack:1142172783825145876>"),
			Map.entry("dfs", "<:defense:1142172792448614450>"),
			Map.entry("ddg", "<:dodge:1142172795242037361>"),
			Map.entry("blk", "<:block:1142172786605961226>")
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
