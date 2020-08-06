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

import java.util.Arrays;
import java.util.List;

import static com.kuuhaku.handlers.games.tabletop.entity.Spot.*;

public class King extends Piece {
	public King(Player owner) {
		super(owner, PieceIcon.KING);
	}

	@Override
	public boolean validate(Board b, Spot to) {
		if (b.getSpot(to) != null && b.getSpot(to).getOwner().equals(getOwner())) return false;

		if (Arrays.stream(new int[][]{UPPER_LEFT, UP, UPPER_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT, LOWER_LEFT, DOWN, LOWER_RIGHT}).anyMatch(i -> getSpot().getNextSpot(i).equals(to))) {
			return true;
		}

		if (isFirstMove()) {
			List<Piece> pieces = Arrays.asList(b.getRow(getSpot().getY()));
			if (to.equals(getSpot().getNextSpot(new int[]{-2, 0}))) {
				boolean roqueL = pieces.subList(0, pieces.indexOf(this)).stream().noneMatch(p -> p != null && p.anyMatch(Knight.class, Bishop.class, Queen.class)) && (pieces.get(0) instanceof Rook && pieces.get(0).isFirstMove());
				return roqueL && isFirstMove();
			} else if (to.equals(getSpot().getNextSpot(new int[]{2, 0}))) {
				boolean roqueR = pieces.subList(pieces.indexOf(this), pieces.size()).stream().noneMatch(p -> p != null && p.anyMatch(Knight.class, Bishop.class)) && (pieces.get(pieces.size() - 1) instanceof Rook && pieces.get(pieces.size() - 1).isFirstMove());
				return roqueR && isFirstMove();
			}
		}

		return false;
	}
}
