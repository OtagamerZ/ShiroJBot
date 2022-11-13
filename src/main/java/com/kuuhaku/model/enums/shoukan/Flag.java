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
	/** Cannot die **/
	NO_DEATH,
	/** Cannot receive equipment stats **/
	NO_EQUIP,
	/** Cannot be converted **/
	NO_CONVERT,
	/** Cannot attack **/
	NO_COMBAT,
	/** Cannot activate effect **/
	NO_EFFECT,
	/** Does not take damage from combat **/
	NO_DAMAGE,
	/** Cannot sleep **/
	NO_SLEEP,
	/** Cannot be stunned **/
	NO_STUN,
	/** Cannot enter stasis **/
	NO_STASIS,
	/** Cannot be taunted **/
	NO_TAUNT,

	/** Ignores target equipment stats **/
	IGNORE_EQUIP,
	/** Ignores field modifiers (NO AUTO) **/
	IGNORE_FIELD,
	/** Cannot be attacked **/
	IGNORE_COMBAT,
	/** Cannot be targeted by effects **/
	IGNORE_EFFECT,

	/** Stats are hidden (NO AUTO) **/
	HIDE_STATS,
	/** Hit chance is reduced by 25% **/
	BLIND,
	/** Will hit **/
	TRUE_STRIKE,
	/** Will dodge **/
	TRUE_DODGE,
	/** Will block **/
	TRUE_BLOCK,
	/** Can attack directly regardless of remaining cards **/
	DIRECT,
	/** Effect has 50% more power and targets nearby cards **/
	EMPOWERED,
	/** Does not consume an action **/
	FREE_ACTION,
	/** Can attack while defending **/
	ALWAYS_ATTACK
}
