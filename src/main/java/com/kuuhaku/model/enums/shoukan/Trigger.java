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

public enum Trigger {
	// Triggers on render
	ON_TICK,

	// Triggers on turn change
	ON_TURN_BEGIN,
	ON_TURN_END,

	// Triggers on each phase
	ON_PLAN,
	ON_COMBAT,

	// Triggers on win condition
	ON_WIN,
	ON_DEFEAT,

	// Triggers before combat
	ON_ATTACK,
	ON_DEFEND,

	// Triggers after combat
	ON_HIT,
	ON_BLOCK,
	ON_DODGE,
	ON_CLASH,
	ON_LOSE,
	ON_SUICIDE,
	ON_MISS,

	// Triggers when sacrificing a card
	ON_SACRIFICE,

	// Triggers when added to each stack
	ON_GRAVEYARD,
	ON_BAN,
	ON_HAND,
	ON_DECK,
	ON_FIELD_CHANGE,

	// Triggers when the card is placed on the field
	ON_INITIALIZE,

	// Triggers when the card is removed from the field
	ON_REMOVE,

	// Triggers on player action
	ON_DRAW,
	ON_DISCARD,
	ON_SUMMON,
	ON_EQUIP,
	ON_SWITCH,
	ON_FLIP,

	// Triggers on player HP change
	ON_HEAL,
	ON_DAMAGE,

	// Single-use trigger
	ON_ACTIVATE,

	// Triggered when targeted by effects
	ON_SPELL_TARGET,
	ON_EFFECT_TARGET,

	// Trigger passed from another card
	ON_DEFER,
	ON_LEECH,

	// Used for nothing
	NONE
}
