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

public class Bishop extends Piece {
	public Bishop(Player owner) {
		super(owner, PieceIcon.BISHOP);
	}

	@Override
	public boolean validate(Board b, Spot to) {
		boolean blocked = false;

		if (b.getSpot(to) != null && b.getSpot(to).getOwner().equals(getOwner())) return false;
		else if (!(b.getSpot(to) instanceof King)) try {
			King k = b.getPieceByType(King.class, getOwner()).get(0);
			if (k.check(b, k.getSpot())) return false;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}

		if (Math.abs(to.getX() - getSpot().getX()) == Math.abs(to.getY() - getSpot().getY())) {
			for (int x = getSpot().getX(), y = getSpot().getY();
				 to.getX() > getSpot().getX() ? x < to.getX() : x > to.getX() && to.getY() > getSpot().getY() ? y < to.getY() : y > to.getY();
			) {
				if (x != getSpot().getX() && y != getSpot().getY()) {
					Piece p = b.getSpot(Spot.of(x, y));

					if (x == to.getX() && y == to.getY()) blocked = p != null && p.getOwner().equals(getOwner());
					else blocked = p != null;
				}

				if (to.getX() > getSpot().getX()) {
					x++;
				} else {
					x--;
				}

				if (to.getY() > getSpot().getY()) {
					y++;
				} else {
					y--;
				}
			}
		} else return false;

		return !blocked;
	}
}
