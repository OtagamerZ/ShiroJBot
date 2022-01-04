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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.model.enums.RankedQueue;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.records.DuoLobby;
import com.kuuhaku.model.records.RankedDuo;
import com.kuuhaku.model.records.SoloLobby;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MatchMaking {
	private final Set<SoloLobby> soloLobby = new LinkedHashSet<>();
	private final Set<DuoLobby> duoLobby = new LinkedHashSet<>();
	private final List<GlobalGame> games = new ArrayList<>();
	private boolean locked = false;

	public Set<SoloLobby> getSoloLobby() {
		return soloLobby;
	}

	public Set<DuoLobby> getDuoLobby() {
		return duoLobby;
	}

	public void joinLobby(User user, User duo, RankedQueue queue, TextChannel channel) {
		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(user.getId());

		switch (queue) {
			case SOLO -> soloLobby.add(new SoloLobby(mmr, channel));
			case DUO -> duoLobby.add(new DuoLobby(new RankedDuo(user, duo), channel));
		}
	}

	public void joinLobby(MatchMakingRating mmr, MatchMakingRating duo, RankedQueue queue, TextChannel channel) {
		switch (queue) {
			case SOLO -> soloLobby.add(new SoloLobby(mmr, channel));
			case DUO -> duoLobby.add(new DuoLobby(new RankedDuo(mmr, duo), channel));
		}
	}

	public void joinLobby(RankedDuo duo, RankedQueue queue, TextChannel channel) {
		if (queue == RankedQueue.DUO) {
			duoLobby.add(new DuoLobby(duo, channel));
		}
	}

	public boolean inGame(String id) {
		closeGames();

		for (GlobalGame g : games) {
			if (g.getBoard().getPlayers().stream().anyMatch(p -> p.getId().equals(id)))
				return true;
		}

		return false;
	}

	public void closeGames() {
		games.removeIf(g -> !g.isOpen());
	}

	public List<GlobalGame> getGames() {
		closeGames();
		return games;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
