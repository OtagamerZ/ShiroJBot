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

package com.kuuhaku.handlers.games.tabletop.entity;

import com.kuuhaku.handlers.games.tabletop.enums.Board;
import com.kuuhaku.handlers.games.tabletop.enums.PieceIcon;
import com.kuuhaku.handlers.games.tabletop.pieces.King;
import com.kuuhaku.handlers.games.tabletop.pieces.Pawn;
import com.kuuhaku.handlers.games.tabletop.pieces.Queen;
import com.kuuhaku.handlers.games.tabletop.pieces.Rook;
import com.kuuhaku.utils.Helper;

import java.util.Arrays;

public abstract class Piece {
	private final Player owner;
	private final PieceIcon icon;
	private Spot spot;
	private boolean firstMove = true;
	private int doubleMoveAt = -1;

	public Piece(Player owner, PieceIcon icon) {
		this.owner = owner;
		this.icon = icon;
	}

	public Player getOwner() {
		return owner;
	}

	public Spot getSpot() {
		return spot;
	}

	public void setSpot(Spot spot) {
		this.spot = spot;
	}

	public PieceIcon getIcon() {
		return icon;
	}

	public boolean move(Board b, Spot to) {
		if (getSpot() == null) setSpot(to);
		else if (validate(b, to)) {
			if (b.getSpot(to) != null && b.getSpot(to).getOwner().equals(getOwner())) return false;

			b.setSpot(null, getSpot());
			b.setSpot(this, to);
			setSpot(to);

			if (this instanceof King) {
				Piece p = Helper.getOr(b.getSpot(getSpot().getNextSpot(Spot.MIDDLE_LEFT)), b.getSpot(getSpot().getNextSpot(new int[]{-2, 0})));

				if (p instanceof Rook && isFirstMove()) {
					Spot sp = p.getSpot();
					p.setSpot(getSpot().getNextSpot(Spot.MIDDLE_RIGHT));
					b.setSpot(p, getSpot().getNextSpot(Spot.MIDDLE_RIGHT));
					b.setSpot(null, sp);
					p.firstMove = false;
				} else {
					p = Helper.getOr(b.getSpot(getSpot().getNextSpot(Spot.MIDDLE_RIGHT)), b.getSpot(getSpot().getNextSpot(new int[]{2, 0})));
					if (p instanceof Rook && isFirstMove()) {
						Spot sp = p.getSpot();
						p.setSpot(getSpot().getNextSpot(Spot.MIDDLE_LEFT));
						b.setSpot(p, getSpot().getNextSpot(Spot.MIDDLE_LEFT));
						b.setSpot(null, sp);
						p.firstMove = false;
					}
				}
			} else if (this instanceof Pawn) {
				if (getOwner().isWhite()) {
					if (getSpot().getY() == 0) {
						Piece p = new Queen(getOwner());
						p.setSpot(getSpot());
						b.setSpot(p, getSpot());
					}
				} else {
					if (getSpot().getY() == 7) {
						Piece p = new Queen(getOwner());
						p.setSpot(getSpot());
						b.setSpot(p, getSpot());
					}
				}
			}

			firstMove = false;
			return true;
		}
		return false;
	}

	public int[] distanceTo(Spot to) {
		return new int[]{to.getX() - spot.getX(), to.getY() - spot.getY()};
	}

	public abstract boolean validate(Board b, Spot to);

	public boolean isFirstMove() {
		return firstMove;
	}

	public int getDoubleMoved() {
		return this instanceof Pawn ? doubleMoveAt : -1;
	}

	public void setDoubleMoveRound(int doubleMoveAt) {
		this.doubleMoveAt = doubleMoveAt;
	}

	@SafeVarargs
	public final boolean anyMatch(Class<? extends Piece>... types) {
		return Arrays.stream(types).anyMatch(t -> getClass() == t);
	}
}