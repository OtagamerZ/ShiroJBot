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

package com.kuuhaku.handlers.games.tabletop.framework;

import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PieceBox {
	private final Map<Class<? extends Piece>, List<Piece>> pieces = new HashMap<>();

	@SafeVarargs
	public PieceBox(String id, boolean white, Pair<Class<? extends Piece>, Integer>... pieces) {
		try {
			for (Pair<Class<? extends Piece>, Integer> piece : pieces) {
				for (int i = 0; i < piece.getValue(); i++) {
					List<Piece> storedPieces = this.pieces.getOrDefault(piece.getKey(), new ArrayList<>());
					storedPieces.add(piece.getKey()
							.getConstructor(String.class, boolean.class, String.class)
							.newInstance(id, white, "pieces/" + piece.getKey().getSimpleName().toLowerCase() + ".png"));
					this.pieces.put(piece.getKey(), storedPieces);
				}
			}
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
			//Helper.logger(this.getClass()).error(e + " | " + e.getStacktrace[0]);
		}
	}

	public Piece getPiece(Class<? extends Piece> pieceType, int index) {
		return pieces.get(pieceType).get(index);
	}
}
