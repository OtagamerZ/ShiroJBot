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

public class Rook extends Piece {
	public Rook(Player owner) {
		super(owner, PieceIcon.ROOK);
	}

	@Override
	public boolean validate(Board b, Spot to) {
		boolean blocked = false;

		if (to.getX() - getSpot().getX() != 0 && to.getY() - getSpot().getY() == 0) {
			for (int i = getSpot().getX(); to.getX() > getSpot().getX() ? i <= to.getX() : i >= to.getX(); ) {
				if (i != getSpot().getX()) {
					Piece p = b.getSpot(Spot.of(i, getSpot().getY()));

					if (i == to.getX()) blocked = p != null && p.getOwner().equals(getOwner());
					else blocked = p != null;
				}

				if (to.getX() > getSpot().getX()) {
					i++;
				} else {
					i--;
				}
			}
		} else if (to.getX() - getSpot().getX() == 0 && to.getY() - getSpot().getY() != 0) {
			for (int i = getSpot().getY(); to.getY() > getSpot().getY() ? i <= to.getY() : i >= to.getY(); ) {
				if (i != getSpot().getX()) {
					Piece p = b.getSpot(Spot.of(getSpot().getX(), i));

					if (i == to.getY()) blocked = p != null && p.getOwner().equals(getOwner());
					else blocked = p != null;
				}

				if (to.getY() > getSpot().getY()) {
					i++;
				} else {
					i--;
				}
			}
		} else return false;

		return !blocked;
	}
}
