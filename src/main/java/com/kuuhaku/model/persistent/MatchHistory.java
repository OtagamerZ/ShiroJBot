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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "matchhistory")
public class MatchHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ElementCollection
	private List<String> players = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL)
	private Map<Integer, MatchRound> rounds = new HashMap<>();

	public int getId() {
		return id;
	}

	public List<String> getPlayers() {
		return players;
	}

	public void setPlayers(List<String> players) {
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
}
