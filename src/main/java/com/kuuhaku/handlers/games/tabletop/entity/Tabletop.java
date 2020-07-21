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
import com.kuuhaku.handlers.games.tabletop.interfaces.GameListener;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public abstract class Tabletop implements GameListener {
	private final TextChannel table;
	private final Board board;
	private final Players players;
	private final String id;

	public Tabletop(TextChannel table, Board board, String id, User... players) {
		this.table = table;
		this.board = board;
		this.players = new Players(players);
		this.id = id;
	}

	public TextChannel getTable() {
		return table;
	}

	public Board getBoard() {
		return board;
	}

	public Players getPlayers() {
		return players;
	}

	public String getId() {
		return id;
	}
}
