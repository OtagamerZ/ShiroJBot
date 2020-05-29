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

import static com.kuuhaku.handlers.games.tabletop.entity.Spot.*;

public class Pawn extends Piece {

	public Pawn(Player owner) {
		super(owner, PieceIcon.PAWN);
	}

	@Override
	public boolean validate(Board b, Spot to) {
		if (getOwner().isWhite()) {
			if (to.equals(getSpot().getNextSpot(UPPER_LEFT)) || to.equals(getSpot().getNextSpot(UPPER_RIGHT))) {
				Piece p = b.getSpot(to.getNextSpot(DOWN));

				if (p != null && b.getRound() == p.getDoubleMoved() + 1) {
					b.setSpot(null, p.getSpot());
					return true;
				}
				return b.getSpot(to) != null && !b.getSpot(to).getOwner().equals(getOwner());
			} else if (to.equals(getSpot().getNextSpot(UP)) || (isFirstMove() && to.equals(getSpot().getNextSpot(new int[]{0, -2})))) {
				setDoubleMoveRound(b.getRound());
				return b.getSpot(to) == null;
			} else return false;
		} else {
			if (to.equals(getSpot().getNextSpot(LOWER_LEFT)) || to.equals(getSpot().getNextSpot(LOWER_RIGHT))) {
				Piece p = b.getSpot(to.getNextSpot(UP));

				if (p != null && b.getRound() == p.getDoubleMoved() + 1) {
					b.setSpot(null, p.getSpot());
					return true;
				}
				return b.getSpot(to) != null && !b.getSpot(to).getOwner().equals(getOwner());
			} else if (to.equals(getSpot().getNextSpot(DOWN)) || (isFirstMove() && to.equals(getSpot().getNextSpot(new int[]{0, 2})))) {
				setDoubleMoveRound(b.getRound());
				return b.getSpot(to) == null;
			} else return false;
		}
	}
}
