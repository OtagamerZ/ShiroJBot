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

import static com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor.*;

public class King extends ChessPiece {

	public King(String ownerId, boolean white, String icon) {
		super(ownerId, white, icon);
	}

	@Override
	public boolean validate(Board board, Spot from, Spot to) {
		if (Arrays.equals(from.getCoords(), to.getCoords())) return false;

		boolean firstMove = false;
		if (isWhite() && Arrays.equals(from.getCoords(), new int[]{4, 7})) firstMove = true;
		else if (Arrays.equals(from.getCoords(), new int[]{4, 0})) firstMove = true;

		int[] vector = {to.getX() - from.getX(), to.getY() - from.getY()};

		if (Math.abs(vector[0]) > 2 || Math.abs(vector[1]) > 2) return false;

		Neighbor n = Neighbor.getByVector(vector);

		if (!ArrayUtils.contains(ArrayUtils.addAll(Neighbor.getCommonMoves(), Neighbor.getCastlingMoves()), n))
			return false;

		if (this instanceof EligibleKing && ArrayUtils.contains(Neighbor.getCastlingMoves(), n)) {
			if (n == LARGE_CASTLING) {
				Piece rook = board.getPieceAt(to.getNeighbor(LARGE_CASTLING));
				if (rook instanceof EligibleRook) {
					board.setPieceAt(to.getNeighbor(LARGE_CASTLING), null);
					board.setPieceAt(to.getNeighbor(RIGHT), new Rook(rook.getOwnerId(), rook.isWhite(), rook.getIconPath()));
				} else return false;
			} else {
				Piece rook = board.getPieceAt(to.getNeighbor(RIGHT));
				if (rook instanceof EligibleRook) {
					board.setPieceAt(to.getNeighbor(RIGHT), null);
					board.setPieceAt(to.getNeighbor(LEFT), new Rook(rook.getOwnerId(), rook.isWhite(), rook.getIconPath()));
				} else return false;
			}
		} else return !ArrayUtils.contains(Neighbor.getCastlingMoves(), n);

		return true;
	}
}