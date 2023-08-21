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

public enum Tag {
	B,
	N,
	PHP("<:hp:1142172799264366703>"),
	BHP("<:base_hp:1142172785049862237>"),
	PMP("<:mp:1142172839412252742>"),
	PDG("<:degen:1142172793811775578>"),
	PRG("<:regen:1142172841668780092>"),
	MP("<:mana:1142172802594635827>"),
	HP("<:blood:1142172788703113257>"),
	ATK("<:attack:1142172783825145876>"),
	DFS("<:defense:1142172792448614450>"),
	DDG("<:dodge:1142172795242037361>"),
	BLK("<:block:1142172786605961226>"),
	CD("<:cooldown:1142172789982363691>"),
	ALLY("<:ally_target:1142172781308551188>"),
	ENEMY("<:enemy_target:1142172797796356146>");

	private final String emote;

	Tag() {
		this.emote = "";
	}

	Tag(String emote) {
		this.emote = emote;
	}

	@Override
	public String toString() {
		return emote;
	}
}
