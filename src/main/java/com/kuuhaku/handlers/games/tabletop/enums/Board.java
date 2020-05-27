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

package com.kuuhaku.handlers.games.tabletop.enums;

import com.kuuhaku.handlers.games.tabletop.entity.Piece;
import com.kuuhaku.handlers.games.tabletop.entity.Spot;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Board {
	private final Piece[][] board;

	Board(Piece[][] board) {
		this.board = board;
	}

	public static Board SIZE_3X3() {
		return new Board(new Piece[3][3]);
	}

	public static Board SIZE_8X8() {
		return new Board(new Piece[8][8]);
	}

	public Piece[][] getLayout() {
		return board;
	}

	public Piece[] getColumn(int index) {
		Piece[] column = new Piece[board.length];
		for (int i = 0; i < board.length; i++) {
			column[i] = board[i][index];
		}
		return column;
	}

	public Piece[] getRow(int index) {
		return board[index];
	}

	public Piece[] getCrossSection(boolean toRight) {
		Piece[] cross = new Piece[board.length];
		if (toRight)
			for (int i = 0; i < board.length; i++) {
				cross[i] = board[i][i];
			}
		else
			for (int i = 0; i < board.length; i++) {
				cross[i] = board[i][(board.length - 1) - i];
			}
		return cross;
	}

	public BufferedImage render() {
		BufferedImage bi = new BufferedImage(64 * board.length, 64 * board.length, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
		for (int y = 0; y < board.length; y++) {
			for (int x = 0; x < board.length; x++) {
				g2d.setColor((y + x) % 2 == 0 ? Color.WHITE : Color.DARK_GRAY);
				g2d.fillRect(64 * x, 64 * y, 64, 64);
				g2d.setColor((y + x) % 2 == 0 ? Color.DARK_GRAY : Color.WHITE);
				Piece p = board[y][x];
				if (p != null) {
					g2d.drawImage(p.getIcon().render(p.getOwner().isWhite()), 64 * x, 64 * y, null);
				} else {
					g2d.drawString(String.valueOf(Spot.getAlphabet().toUpperCase().charAt(x)) + (y + 1), 64 * x + 13, 64 * y + 44);
				}
			}
		}
		g2d.dispose();
		return bi;
	}
}
