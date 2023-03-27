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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.persistent.shoukan.Evogear;

public class TrapSpell extends PlaceableEvogear {
	public TrapSpell(Evogear e) {
		super(e.copy());
		setFlipped(true);
	}

	@Override
	public void setFlipped(boolean flipped) {
		super.setFlipped(flipped);

		if (!flipped) {
			Shoukan game = getHand().getGame();
			game.getChannel().sendMessage(game.getLocale().get("str/trap_disarm", getOriginal())).queue();
			getHand().getGraveyard().add(this);
		}
	}

	@Override
	public boolean canAttack() {
		return false;
	}
}
