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
	ON_FLIP(false, true),
	ON_ATTACK(false, true),
	ON_SUMMON(false, true),
	BEFORE_TURN(false, false),
	AFTER_TURN(false, false),
	ON_SWITCH(false, true),
	ON_SUICIDE(false, true),
	ON_EQUIP(false, true),
	POST_ATTACK(false, true),
	ATTACK_ASSIST(false, true),
	POST_DEFENSE_ASSIST(false, true),
	POST_ATTACK_ASSIST(false, true),
	ON_MISS(false, true),
	GAME_TICK(false, false),
	GLOBAL_TICK(false, true),
	ON_DESTROY(false, true),
	ON_LOSE(false, false),
	ON_WIN(false, false),
	ON_HEAL(false, true),
	ON_DAMAGE(false, true),
	ON_OP_HEAL(false, true),
	ON_OP_DAMAGE(false, true),
	ON_MANUAL_DRAW(false, true),
	ON_OP_MANUAL_DRAW(false, true),
	ON_DRAW(false, true),
	ON_OP_DRAW(false, true),
	ON_DISCARD(false, true),
	ON_OP_DISCARD(false, true),
	FINALIZE(false, true),

	BEFORE_DEATH(true, true),
	AFTER_DEATH(true, true),
	ON_DEFEND(true, true),
	POST_DEFENSE(true, true),
	DEFENSE_ASSIST(true, true),
	ON_DODGE(true, true);

	private final boolean defensive;
	private final boolean individual;

	EffectTrigger(boolean defensive, boolean individual) {
		this.defensive = defensive;
		this.individual = individual;
	}

	public boolean isDefensive() {
		return defensive;
	}

	public boolean isIndividual() {
		return individual;
	}
}
