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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

public enum ArenaField {
	DEFAULT/*(Map.of())*/,
	NEW_WORLD/*(Map.of())*/,
	GRAVEYARD/*(Map.of(
			Race.UNDEAD, 1.5f,
			Race.SPIRIT, 1.25f,
			Race.MYSTICAL, 0.75f
	))*/,
	UNDERWORLD/*(Map.of(
			Race.DEMON, 1.5f,
			Race.SPIRIT, 0.75f,
			Race.DIVINITY, 0.5f
	))*/,
	ECLIPSE/*(Map.of(
			Race.DIVINITY, 1.5f,
			Race.MYSTICAL, 1.25f,
			Race.DEMON, 0.75f
	))*/,
	FOREST/*(Map.of(
			Race.MYSTICAL, 1.5f,
			Race.CREATURE, 1.5f,
			Race.ELF, 1.25f,
			Race.BESTIAL, 1.25f
	))*/
}
