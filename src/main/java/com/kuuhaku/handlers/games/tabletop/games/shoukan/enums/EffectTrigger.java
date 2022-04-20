/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

public enum EffectTrigger {
	NONE(false, false),

	// GLOBAL
	GAME_TICK(false, false),
	GLOBAL_TICK(false),

	BEFORE_TURN(false, false), // Is triggered individually
	PLAN_STAGE(false),
	COMBAT_STAGE(false),
	AFTER_TURN(false, false), // Is triggered individually

	ON_LOSE(false, false),
	ON_WIN(false, false),
	ON_GLOBAL_LOSE(false),
	ON_GLOBAL_WIN(false),
	// ------

	// CARD
	ON_FLIP(false),
	ON_SWITCH(false),
	ON_EQUIP(false),
	// ----

	// SLOT
	ON_SUMMON(false),
	FINALIZE(false),
	// ----

	// COMBAT
	ON_ATTACK(false),
	ON_MISS(false),
	ON_SUICIDE(false),
	POST_ATTACK(false),
	ATTACK_ASSIST(false),
	POST_ATTACK_ASSIST(false),

	ON_DEFEND(true),
	ON_DODGE(true),
	ON_DEATH(true),
	AFTER_DEATH(true),
	POST_DEFENSE(true),
	DEFENSE_ASSIST(true),
	POST_DEFENSE_ASSIST(true),
	// ------

	// HAND
	ON_SELF_DISCARD(false, false),

	ON_DAMAGE(false),
	ON_HEAL(false),
	ON_MANUAL_DRAW(false),
	ON_DRAW(false),
	ON_DISCARD(false),

	ON_OP_DAMAGE(false),
	ON_OP_HEAL(false),
	ON_OP_MANUAL_DRAW(false),
	ON_OP_DRAW(false),
	ON_OP_DISCARD(false),
	// ----

	// GRAVEYARD
	ON_SACRIFICE(false),
	ON_GRAVEYARD(true),
	// ---------
	;

	private final boolean defensive;
	private final boolean triggerPE;

	EffectTrigger(boolean defensive, boolean triggerPE) {
		this.defensive = defensive;
		this.triggerPE = triggerPE;
	}

	EffectTrigger(boolean defensive) {
		this.defensive = defensive;
		this.triggerPE = true;
	}

	public boolean isDefensive() {
		return defensive;
	}

	public boolean shouldTriggerPE() {
		return triggerPE;
	}
}
