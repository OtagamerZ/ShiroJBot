/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.games.rpg.enums;

public enum Resource {
	MONEY(new String[]{
			"gold",
			"money",
			"coin",
			"coins",
			"ouro",
			"dinheiro",
			"moeda",
			"moedas"
	}),
	XP(new String[]{
			"xp",
			"experience",
			"exp",
			"experiencia"
	}),
	HEALTH(new String[]{
			"life",
			"health",
			"hp",
			"vida",
			"saude"
	});

	private final String[] aliases;

	Resource(String[] aliases) {
		this.aliases = aliases;
	}

	public String[] getAliases() {
		return aliases;
	}
}
