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

package com.kuuhaku.handlers.games.tabletop.games.chess.pieces;

import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Piece;
import com.kuuhaku.handlers.games.tabletop.framework.Spot;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class Knight extends ChessPiece {

	public Knight(String ownerId, boolean white, String icon) {
		super(ownerId, white, icon);
	}

	@Override
	public boolean validate(Board board, Spot from, Spot to) {
		if (Arrays.equals(from.getCoords(), to.getCoords())) return false;

		int[] vector = {to.getX() - from.getX(), to.getY() - from.getY()};
		Neighbor n = Neighbor.getByVector(vector);

		if (!ArrayUtils.contains(Neighbor.getKnightMoves(), n)) return false;

		Piece atSpot = board.getPieceAt(to);

		return atSpot == null || !atSpot.getOwnerId().equals(getOwnerId());
	}
}