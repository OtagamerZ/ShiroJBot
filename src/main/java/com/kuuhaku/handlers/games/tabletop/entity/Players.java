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

import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Players {
	private final LinkedList<User> turn;
	private User winner = null;
	private User loser = null;

	public Players(User... players) {
		this.turn = new LinkedList<>(Arrays.asList(players));
	}

	public User nextTurn() {
		User u = turn.poll();
		turn.offer(u);
		return u;
	}

	public void remove(User u) {
		turn.remove(u);
	}

	public User getWinner() {
		return winner;
	}

	public User getLoser() {
		return loser;
	}

	public void setWinner(User winner) {
		if (winner == null) return;
		this.winner = winner;
		this.loser = turn.stream().filter(u -> !u.getId().equals(winner.getId())).findFirst().orElseThrow();
	}

	public List<User> getUsers() {
		return turn;
	}

	public LinkedList<User> getUserSequence() {
		return turn;
	}
}
