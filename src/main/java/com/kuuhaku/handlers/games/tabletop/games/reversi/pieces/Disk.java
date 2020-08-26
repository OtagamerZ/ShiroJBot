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

package com.kuuhaku.handlers.games.tabletop.games.reversi.pieces;

import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Piece;
import com.kuuhaku.handlers.games.tabletop.framework.Spot;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.framework.interfaces.Condition;

import java.util.Arrays;

import static com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor.*;

public class Disk extends Piece implements Condition {

	public Disk(String ownerId, boolean white, String icon) {
		super(ownerId, white, icon);
	}

	@Override
	public boolean validate(Board board, Spot from, Spot to) {
		Neighbor[] directions = {UP, UPPER_RIGHT, RIGHT, LOWER_RIGHT, DOWN, LOWER_LEFT, LEFT, UPPER_LEFT};

		int distance = -1;
		Neighbor shortestRoute = null;
		for (Neighbor n : directions) {
			Piece[] toCheck = board.getLine(from, n, false);
			boolean foundPair = Arrays.stream(toCheck).anyMatch(p -> p != null && p.isWhite() == isWhite());
			int untilPair = -1;
			if (foundPair) {
				for (int i = 0; i < toCheck.length; i++) {
					Piece p = toCheck[i];
					if (p == null) break;
					else if (p.isWhite() == isWhite() && i > 0) {
						untilPair = i;
						break;
					}
				}

				if (untilPair > -1) {
					shortestRoute = n;
					if (distance == -1) distance = untilPair;
					else distance = Math.min(distance, untilPair);
				}
			}
		}

		if (shortestRoute == null) return false;
		else {
			board.fillLine(from, distance, this, shortestRoute, false);
			return true;
		}
	}
}
