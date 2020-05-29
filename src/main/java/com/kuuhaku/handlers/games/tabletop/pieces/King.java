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
			return true;//!check(b, to);
		}

		if (isFirstMove()) {
			List<Piece> pieces = Arrays.asList(b.getRow(getSpot().getY()));
			if (to.equals(getSpot().getNextSpot(new int[]{-3, 0}))) {
				boolean roqueL = pieces.subList(0, pieces.indexOf(this)).stream().noneMatch(p -> p != null && p.anyMatch(Knight.class, Bishop.class, Queen.class)) && (pieces.get(0) instanceof Rook && pieces.get(0).isFirstMove());
				return roqueL && isFirstMove();
			} else if (to.equals(getSpot().getNextSpot(new int[]{2, 0}))) {
				boolean roqueR = pieces.subList(pieces.indexOf(this), pieces.size()).stream().noneMatch(p -> p != null && p.anyMatch(Knight.class, Bishop.class)) && (pieces.get(pieces.size() - 1) instanceof Rook && pieces.get(pieces.size() - 1).isFirstMove());
				return roqueR && isFirstMove();
			}
		}

		return false;
	}

	public boolean check(Board b, Spot spot) {
		boolean hCheck = false;
		boolean vCheck = false;
		boolean dCheck = false;
		boolean kCheck = false;
		boolean pCheck = false;

		if (Arrays.stream(b.getRow(spot.getY())).anyMatch(p -> p != null && !p.getOwner().equals(getOwner()) && p.anyMatch(Rook.class, Queen.class)))
			hCheck = true;
		else if (Arrays.stream(b.getColumn(spot.getX())).anyMatch(p -> p != null && !p.getOwner().equals(getOwner()) && p.anyMatch(Rook.class, Queen.class)))
			vCheck = true;
		else if (Arrays.stream(b.getCrossSection(spot.getX(), true)).anyMatch(p -> p != null && !p.getOwner().equals(getOwner()) && p.anyMatch(Bishop.class, Queen.class)))
			dCheck = true;
		else if (Arrays.stream(b.getCrossSection(spot.getX(), false)).anyMatch(p -> p != null && !p.getOwner().equals(getOwner()) && p.anyMatch(Bishop.class, Queen.class)))
			dCheck = true;
		else {
			if (getOwner().isWhite()) for (int[] pos : new int[][]{UPPER_LEFT, UPPER_RIGHT}) {
				Piece p = b.getSpot(spot.getNextSpot(pos));
				pCheck = p instanceof Pawn && !p.getOwner().equals(getOwner());
			}
			else for (int[] pos : new int[][]{LOWER_LEFT, LOWER_RIGHT}) {
				Piece p = b.getSpot(spot.getNextSpot(pos));
				pCheck = p instanceof Pawn && !p.getOwner().equals(getOwner());
			}

			for (int[] pos : new int[][]{{-1, -2}, {-2, -1}, {1, -2}, {2, -1}, {-1, 2}, {-2, 1}, {1, 2}, {2, 1}}) {
				Piece p = b.getSpot(spot.getNextSpot(pos));
				kCheck = p instanceof Knight && !p.getOwner().equals(getOwner());
			}
		}

		return Arrays.stream(new Boolean[]{hCheck, vCheck, dCheck, kCheck, pCheck}).anyMatch(bo -> bo);
	}
}
