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
import com.kuuhaku.handlers.games.tabletop.utils.Helper;
import org.apache.commons.lang3.ArrayUtils;

import java.math.RoundingMode;
import java.util.Arrays;

import static com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor.*;

public class Bishop extends ChessPiece {

	public Bishop(String ownerId, boolean white, String icon) {
		super(ownerId, white, icon);
	}

	@Override
	public boolean validate(Board board, Spot from, Spot to) {
		if (Arrays.equals(from.getCoords(), to.getCoords())) return false;

		int[] vector = {to.getX() - from.getX(), to.getY() - from.getY()};
		int[] normVector = Helper.normalize(vector, RoundingMode.CEILING);
		Neighbor n = Neighbor.getByVector(normVector);

		if (!ArrayUtils.contains(new Neighbor[]{LOWER_LEFT, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT}, n)) return false;

		boolean lastPass = false;
		for (Spot i = from.getNeighbor(n); !Arrays.equals(i.getCoords(), to.getCoords()) && !lastPass; i = i.getNeighbor(n)) {
			lastPass = Arrays.equals(i.getCoords(), to.getCoords());

			Piece atSpot = board.getPieceAt(i);
			if (atSpot != null) {
				if (lastPass) return !atSpot.getOwnerId().equals(getOwnerId());
				else return false;
			}
		}

		return true;
	}
}
