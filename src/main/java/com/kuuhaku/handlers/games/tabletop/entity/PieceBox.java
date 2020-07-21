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

import com.kuuhaku.handlers.games.tabletop.pieces.*;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;

public class PieceBox {
	private final User user;
	private final List<Piece> pieces = new ArrayList<>();

	public PieceBox(User user, boolean isWhite) {
		this.user = user;
		this.pieces.addAll(new ArrayList<>() {{
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));
			add(new Pawn(new Player(user, isWhite)));

			add(new Rook(new Player(user, isWhite)));
			add(new Knight(new Player(user, isWhite)));
			add(new Bishop(new Player(user, isWhite)));
			add(new Queen(new Player(user, isWhite)));
			add(new King(new Player(user, isWhite)));
			add(new Bishop(new Player(user, isWhite)));
			add(new Knight(new Player(user, isWhite)));
			add(new Rook(new Player(user, isWhite)));
		}});
	}

	public User getUser() {
		return user;
	}

	public List<Piece> getPieces() {
		return pieces;
	}
}
