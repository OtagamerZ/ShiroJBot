/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.pieces;

import com.kuuhaku.handlers.games.tabletop.entity.Piece;
import com.kuuhaku.handlers.games.tabletop.entity.Player;
import com.kuuhaku.handlers.games.tabletop.entity.Spot;
import com.kuuhaku.handlers.games.tabletop.enums.Board;
import com.kuuhaku.handlers.games.tabletop.enums.PieceIcon;

public class Queen extends Piece {
	public Queen(Player owner) {
		super(owner, PieceIcon.QUEEN);
	}

	@Override
	public boolean validate(Board b, Spot to) {
		try {
			King k = b.getPieceByType(King.class, getOwner()).get(0);
			if (k.check(b, k.getSpot())) return false;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}

		Piece rk = new Rook(getOwner());
		Piece bs = new Bishop(getOwner());

		rk.setSpot(getSpot());
		bs.setSpot(getSpot());

		return rk.validate(b, to) || bs.validate(b, to);
	}
}
