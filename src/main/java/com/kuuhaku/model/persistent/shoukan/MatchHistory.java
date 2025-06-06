/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.persistent.shoukan.history.HistoryInfo;
import com.kuuhaku.model.persistent.shoukan.history.HistoryTurn;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "match_history", schema = "shiro")
public class MatchHistory extends DAO<MatchHistory> {
	@Id
	@Column(name = "id", nullable = false)
	private int id;

	@OneToOne(cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumn(name = "id")
	@Fetch(FetchMode.JOIN)
	private HistoryInfo info;

	@OneToMany(mappedBy = "parent", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private Set<HistoryTurn> turns = new LinkedHashSet<>();

	public MatchHistory() {
	}

	public MatchHistory(Shoukan game, String winCondition, List<HistoryTurn> turns) {
		DAO.applyNative(null, "CREATE SEQUENCE IF NOT EXISTS history_match_id_seq");

		this.id = DAO.queryNative(Integer.class, "SELECT nextval('history_match_id_seq')");
		this.info = new HistoryInfo(this, game, winCondition);
		for (HistoryTurn turn : turns) {
			this.turns.add(turn.parent(this));
		}
	}

	public int getId() {
		return id;
	}

	public HistoryInfo getInfo() {
		return info;
	}

	public Set<HistoryTurn> getTurns() {
		return turns;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MatchHistory that = (MatchHistory) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
