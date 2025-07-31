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

package com.kuuhaku.model.enums.shoukan;

import java.util.EnumSet;
import java.util.Set;

public enum Trigger {
	/**
	 * On each render<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_TICK,

	/**
	 * When turn begins (also expires turn-based effects)<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_TURN_BEGIN,

	/**
	 * When turn ends<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_TURN_END,

	/**
	 * When PLAN phase begins
	 **/
	ON_PLAN,

	/**
	 * When COMBAT phase begins<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_COMBAT,

	/**
	 * When victory is imminent<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_VICTORY,

	/**
	 * When defeat is imminent
	 **/
	ON_DEFEAT,

	/**
	 * Before attack calculations<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_ATTACK,

	/**
	 * Before defense calculations<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_DEFEND,

	/**
	 * On defeating a card<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_HIT,

	/**
	 * On sending a card to the graveyard<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_KILL,

	/**
	 * After sending a card to the graveyard
	 **/
	ON_CONFIRMED_KILL,

	/**
	 * On attacking directly
	 **/
	ON_DIRECT,

	/**
	 * On successful parry<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_PARRY,

	/**
	 * On successful dodge<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_DODGE,

	/**
	 * When both sides have equal attributes
	 **/
	ON_CLASH,

	/**
	 * On losing the combat
	 **/
	ON_LOSE,

	/**
	 * On failing an attack
	 **/
	ON_SUICIDE,

	/**
	 * On missing an attack<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_MISS,

	/**
	 * On sending a placed card to graveyard
	 **/
	ON_SACRIFICE,

	/**
	 * On being added to the graveyard pile<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_GRAVEYARD,

	/**
	 * On being added to the ban pile
	 **/
	ON_BAN,

	/**
	 * On being added to the hand pile
	 **/
	ON_HAND,

	/**
	 * On being added to the deck pile
	 **/
	ON_DECK,

	/**
	 * On changing the field
	 **/
	ON_FIELD_CHANGE,

	/**
	 * On being placed <b>(Asserted)</b><br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_INITIALIZE,

	/**
	 * On being removed <b>(Asserted)</b><br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_REMOVE,

	/**
	 * On drawing a card
	 **/
	ON_DRAW,

	/**
	 * On drawing a card manually
	 **/
	ON_MANUAL_DRAW,

	/**
	 * On drawing a card through effects
	 **/
	ON_MAGIC_DRAW,

	/**
	 * After drawing a card
	 **/
	ON_DRAW_SINGLE,

	/**
	 * After drawing many cards
	 **/
	ON_DRAW_MULTIPLE,

	/**
	 * On discarding a card
	 **/
	ON_DISCARD,

	/**
	 * On summoning a card
	 **/
	ON_SUMMON,

	/**
	 * On equipping a card
	 **/
	ON_EQUIP,

	/**
	 * On changing card mode (Flip -> Defense -> Attack -> Defense)
	 **/
	ON_SWITCH,

	/**
	 * On flipping a card
	 **/
	ON_FLIP,

	/**
	 * On receiving HP<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_HEAL,

	/**
	 * On healing through regen
	 **/
	ON_REGEN,

	/**
	 * On losing HP<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_DAMAGE,

	/**
	 * On taking degen damage<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_DEGEN,

	/**
	 * On consuming HP
	 **/
	ON_CONSUME_HP,

	/**
	 * On receiving MP
	 **/
	ON_MP,

	/**
	 * On consuming MP
	 **/
	ON_CONSUME_MP,

	/**
	 * On using a card's active effect
	 **/
	ON_ACTIVATE,

	/**
	 * On using a spell<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_SPELL,

	/**
	 * On using an ability
	 **/
	ON_ABILITY,

	/**
	 * On having flags altered
	 **/
	ON_FLAG_ALTER,

	/**
	 * When targeted by a spell<br>
	 * <i>Implemented in DunHun</i>
	 **/
	ON_SPELL_TARGET,

	/**
	 * When targeted
	 **/
	ON_EFFECT_TARGET,

	/**
	 * When a proxied spell is activated
	 **/
	ON_TRAP,

	/**
	 * Triggers mirrored from supported card
	 **/
	ON_DEFER_SUPPORT,

	/**
	 * Triggers mirrored from adjacent cards
	 **/
	ON_DEFER_NEARBY,

	/**
	 * Triggers forwarded from binding
	 */
	ON_DEFER_BINDING,

	/**
	 * Triggers when the actor revives<br>
	 * <i>Only in DunHun</i>
	 */
	ON_REVIVE,

	/**
	 * Nothing <b>(DO NOT USE)</b>
	 **/
	NONE;

	public static Set<Trigger> getAnnounceable() {
		return EnumSet.complementOf(EnumSet.of(
				ON_TICK, ON_INITIALIZE, ON_REMOVE, NONE
		));
	}
}
