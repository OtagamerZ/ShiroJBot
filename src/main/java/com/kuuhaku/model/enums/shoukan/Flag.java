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

package com.kuuhaku.model.enums.shoukan;

public enum Flag {
	NO_DEATH,   // Cannot die
	NO_EQUIP,   // Cannot receive equipment stats
	NO_CONVERT, // Cannot be converted
	NO_COMBAT,  // Cannot attack
	NO_EFFECT,  // Cannot activate effect
	NO_DAMAGE,  // Does not take damage from combat

	IGNORE_EQUIP,  // Ignores target equipment stats
	IGNORE_FIELD,  // Ignores field modifiers (NO AUTO)
	IGNORE_COMBAT, // Cannot be attacked
	IGNORE_EFFECT, // Cannot be targeted by effects

	HIDE_STATS,  // Stats are hidden (NO AUTO)
	BLIND,		 // Hit chance is reduced by 25%
	TRUE_STRIKE, // Will hit
	TRUE_DODGE,  // Will dodge
	TRUE_BLOCK,  // Will block
	DIRECT,      // Can attack directly regardless of remaining cards
}
