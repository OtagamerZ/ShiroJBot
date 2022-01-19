/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.normal.framework;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.InfiniteList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Table {
	private final InfiniteList<Player> players;
	private boolean awarded = false;

	public Table(long bet, String... players) {
		this.players = Arrays.stream(players).map(s -> new Player(s, bet, AccountDAO.getAccount(s).getLoan() > 0)).collect(Collectors.toCollection(InfiniteList::new));

		Collections.reverse(this.players);
	}

	public InfiniteList<Player> getPlayers() {
		return players;
	}

	public InfiniteList<Player> getInGamePlayers() {
		return players.stream().filter(Player::isInGame).collect(Collectors.toCollection(InfiniteList::new));
	}

	public void leaveGame() {
		Objects.requireNonNull(players.pollFirst()).setInGame(false);
	}

	public void awardWinner(Game game, String id) {
		List<Player> losers = players.stream().filter(p -> !p.getId().equals(id)).toList();

		Account wacc = AccountDAO.getAccount(id);
		wacc.addCredit(losers.stream().mapToLong(Player::getBet).sum(), game.getClass());
		AccountDAO.saveAccount(wacc);

		for (Player l : losers) {
			Account lacc = AccountDAO.getAccount(l.getId());
			lacc.removeCredit(l.hasLoan() ? l.getBet() * 2 : l.getBet(), game.getClass());
			AccountDAO.saveAccount(lacc);
		}

		awarded = true;
	}

	public boolean isAwarded() {
		return awarded;
	}
}
