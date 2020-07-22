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

package com.kuuhaku.handlers.games.framework;

import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Players {
	private final Map<Integer, User> players = new HashMap<>();
	private int current = 0;
	private int winner = -1;
	private Set<User> losers = new HashSet<>();

	public Players(User... players) {
		for (int i = 0; i < players.length; i++)
			this.players.put(i, players[i]);
	}

	public User nextTurn() {
		while (!players.containsKey(current)) {
			current++;
			if (current >= players.size()) current = 0;
		}
		return players.get(current);
	}

	public User peekNext() {
		int next = current + 1;
		while (!players.containsKey(next)) {
			next++;
			if (next >= players.size()) next = 0;
		}
		return players.get(next);
	}

	public void lastOneStarts() {
		current = players.size() - 1;
	}

	public User getWinner() {
		return players.get(winner);
	}

	public Set<User> getLosers() {
		return losers;
	}

	public void setWinner() {
		winner = current;
	}

	public void setWinner(User u) {
		if (u == null) return;
		for (Map.Entry<Integer, User> entry : players.entrySet()) {
			if (entry.getValue().getId().equals(u.getId())) {
				winner = entry.getKey();
				break;
			}
		}
	}

	public void setLoser() {
		losers.add(players.remove(current));
	}

	public Map<Integer, User> getPlayers() {
		return players;
	}

	public User getCurrent() {
		return players.get(current);
	}
}
