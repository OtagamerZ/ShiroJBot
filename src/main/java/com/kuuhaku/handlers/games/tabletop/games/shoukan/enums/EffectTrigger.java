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
	NONE(false),
	ON_FLIP(false),
	ON_ATTACK(false),
	ON_SUMMON(false),
	BEFORE_TURN(false),
	AFTER_TURN(false),
	ON_SWITCH(false),
	ON_SUICIDE(false),
	ON_EQUIP(false),
	POST_ATTACK(false),
	ATTACK_ASSIST(false),
	POST_DEFENSE_ASSIST(false),
	POST_ATTACK_ASSIST(false),
	ON_MISS(false),
	GAME_TICK(false),
	ON_DESTROY(false),
	ON_LOSE(false),
	ON_WIN(false),
	ON_HEAL(false),
	ON_DAMAGE(false),

	BEFORE_DEATH(true),
	AFTER_DEATH(true),
	ON_DEFEND(true),
	POST_DEFENSE(true),
	DEFENSE_ASSIST(true),
	ON_DODGE(true);

	private final boolean defensive;

	EffectTrigger(boolean defensive) {
		this.defensive = defensive;
	}

	public boolean isDefensive() {
		return defensive;
	}
}
