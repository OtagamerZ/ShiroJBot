/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "matchhistory")
public class MatchHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(value = EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	@JoinColumn(name = "matchhistory_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Map<String, Side> players = new HashMap<>();

	@Enumerated(value = EnumType.STRING)
	private Side winner = null;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean ranked = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean wo = false;

	@OneToMany(mappedBy = "history", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@Cascade(org.hibernate.annotations.CascadeType.ALL)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Map<Integer, MatchRound> rounds = new HashMap<>();

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime timestamp = ZonedDateTime.now(ZoneId.of("GMT-3"));

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
		return rounds.computeIfAbsent(round, k -> new MatchRound());
	}

	public void setRounds(Map<Integer, MatchRound> rounds) {
		this.rounds = rounds;
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public Side getWinner() {
		return winner;
	}

	public void setWinner(Side winner) {
		this.winner = winner;
	}

	public boolean isRanked() {
		return ranked;
	}

	public void setRanked(boolean ranked) {
		this.ranked = ranked;
	}

	public boolean isWo() {
		return wo;
	}

	public void setWo(boolean wo) {
		this.wo = wo;
	}
}
