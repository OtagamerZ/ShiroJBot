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

package com.kuuhaku.handlers.games.rpg.handlers;

public class Action {
	private boolean attacking = false;
	private boolean defending = false;
	private boolean lookingBag = false;
	private boolean fleeing = false;

	boolean isAttacking() {
		return attacking;
	}

	void setAttacking() {
		this.attacking = true;
	}

	boolean isDefending() {
		return defending;
	}

	void setDefending() {
		this.defending = true;
	}

	boolean isLookingBag() {
		return lookingBag;
	}

	void setLookingBag(boolean lookingBag) {
		this.lookingBag = lookingBag;
	}

	boolean isFleeing() {
		return fleeing;
	}

	void setFleeing() {
		this.fleeing = true;
	}
}
