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
import com.kuuhaku.handlers.games.tabletop.framework.Decoy;
import com.kuuhaku.handlers.games.tabletop.framework.Piece;
import com.kuuhaku.handlers.games.tabletop.framework.Spot;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.utils.Helper;
import org.apache.commons.lang3.ArrayUtils;

import java.math.RoundingMode;
import java.util.Arrays;

import static com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor.*;

public class Pawn extends ChessPiece {

	public Pawn(String ownerId, boolean white, String icon) {
		super(ownerId, white, icon);
	}

	@Override
	public boolean validate(Board board, Spot from, Spot to) {
		if (Arrays.equals(from.getCoords(), to.getCoords())) return false;

		boolean firstMove = false;
		if (isWhite() && from.getY() == 6) firstMove = true;
		else if (from.getY() == 1) firstMove = true;

		int[] vector = {to.getX() - from.getX(), to.getY() - from.getY()};

		if (Math.abs(vector[1]) > 2) return false;

		int[] normVector = Helper.normalize(vector, RoundingMode.CEILING);
		Neighbor n = Neighbor.getByVector(normVector);

		if (isWhite()) {
			if (!ArrayUtils.contains(new Neighbor[]{UP, UPPER_LEFT, UPPER_RIGHT}, n)) return false;

			if (firstMove) {
				boolean lastPass = false;
				for (Spot i = from.getNeighbor(n); i.getX() != to.getX() && i.getY() != to.getY() && !lastPass; i = i.getNeighbor(n)) {
					lastPass = Arrays.equals(i.getCoords(), to.getCoords());

					Piece atSpot = board.getPieceAt(i);
					if (atSpot != null) {
						return false;
					}
				}

				if (Math.abs(vector[1]) > 1) board.setPieceAt(to.getNeighbor(DOWN), new Decoy(getOwnerId(), isWhite()));
			} else if (Math.abs(vector[1]) > 1) return false;

			Piece atSpot = board.getPieceOrDecoyAt(to);
			if (atSpot != null && n.equals(UP)) return false;
			else if (n.equals(UPPER_LEFT) || n.equals(UPPER_RIGHT)) {
				if (atSpot != null && !atSpot.getOwnerId().equals(getOwnerId())) {
					if (atSpot instanceof Decoy) board.setPieceAt(to.getNeighbor(DOWN), null);
				} else return false;
			}
		} else {
			if (!ArrayUtils.contains(new Neighbor[]{DOWN, LOWER_LEFT, LOWER_RIGHT}, n)) return false;

			if (firstMove) {
				boolean lastPass = false;
				for (Spot i = from.getNeighbor(n); i.getX() != to.getX() && i.getY() != to.getY() && !lastPass; i = i.getNeighbor(n)) {
					lastPass = Arrays.equals(i.getCoords(), to.getCoords());

					Piece atSpot = board.getPieceAt(i);
					if (atSpot != null) {
						return false;
					}
				}

				if (Math.abs(vector[1]) > 1) board.setPieceAt(to.getNeighbor(UP), new Decoy(getOwnerId(), isWhite()));
			} else if (Math.abs(vector[1]) > 1) return false;

			Piece atSpot = board.getPieceOrDecoyAt(to);
			if (atSpot != null && n.equals(DOWN)) return false;
			else if (n.equals(LOWER_LEFT) || n.equals(LOWER_RIGHT)) {
				if (atSpot != null && !atSpot.getOwnerId().equals(getOwnerId())) {
					if (atSpot instanceof Decoy) board.setPieceAt(to.getNeighbor(UP), null);
				} else return false;
			}
		}

		return true;
	}
}