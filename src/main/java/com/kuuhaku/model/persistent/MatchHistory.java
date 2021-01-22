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

package com.kuuhaku.model.persistent;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;

import javax.persistence.*;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Entity
@Table(name = "matchhistory")
public class MatchHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(value = EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, Side> players = new HashMap<>();

	@Enumerated(value = EnumType.STRING)
	private Side winner = null;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private Map<Integer, MatchRound> rounds = new HashMap<>();

	@Temporal(TemporalType.DATE)
	private Calendar timestamp = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));

	public int getId() {
		return id;
	}

	public Map<String, Side> getPlayers() {
		return players;
	}

	public void setPlayers(Map<String, Side> players) {
		this.players = players;
	}

	public Map<Integer, MatchRound> getRounds() {
		return rounds;
	}

	public MatchRound getRound(int round) {
		return rounds.compute(round, (i, mr) -> mr == null ? new MatchRound() : mr);
	}

	public void setRounds(Map<Integer, MatchRound> rounds) {
		this.rounds = rounds;
	}

	public Calendar getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

	public Side getWinner() {
		return winner;
	}

	public void setWinner(Side winner) {
		this.winner = winner;
	}
}
