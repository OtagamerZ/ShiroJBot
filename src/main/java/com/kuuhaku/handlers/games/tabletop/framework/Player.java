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

import com.kuuhaku.Main;
import net.dv8tion.jda.api.entities.User;

public class Player {
	private final String id;
	private final long bet;
	private final boolean hasLoan;
	private boolean inGame = true;

	public Player(String id, long bet, boolean hasLoan) {
		this.id = id;
		this.bet = hasLoan ? bet / 2 : bet;
		this.hasLoan = hasLoan;
	}

	public String getId() {
		return id;
	}

	public User getUser() {
		return Main.getInfo().getUserByID(id);
	}

	public long getBet() {
		return bet;
	}

	public boolean hasLoan() {
		return hasLoan;
	}

	public boolean isInGame() {
		return inGame;
	}

	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}
}
