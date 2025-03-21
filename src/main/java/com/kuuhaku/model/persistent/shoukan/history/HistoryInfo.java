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

package com.kuuhaku.model.persistent.shoukan.history;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.shoukan.Arcade;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shoukan.MatchHistory;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "history_info", schema = "shiro")
public class HistoryInfo {
	@Id
	@Column(name = "match_id", nullable = false)
	private int matchId;

	@OneToOne(mappedBy = "parent", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.JOIN)
	private HistoryPlayer top;

	@OneToOne(mappedBy = "parent", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.JOIN)
	private HistoryPlayer bottom;

	@Enumerated(EnumType.STRING)
	@Column(name = "winner")
	private Side winner;

	@Enumerated(EnumType.STRING)
	@Column(name = "arcade")
	private Arcade arcade;

	@Column(name = "win_condition")
	private String winCondition;

	@Column(name = "match_timestamp", nullable = false)
	private ZonedDateTime timestamp = ZonedDateTime.now(ZoneId.of("GMT-3"));;

	@Column(name = "seed", nullable = false)
	private long seed;

	public HistoryInfo() {
	}

	public HistoryInfo(MatchHistory match, Shoukan game, String winCondition) {
		if (game.isSingleplayer()) {
			this.top = this.bottom = new HistoryPlayer(this, game.getHands().get(Side.TOP));
		} else {
			for (Map.Entry<Side, Hand> e : game.getHands().entrySet()) {
				HistoryPlayer p = new HistoryPlayer(this, e.getValue());

				if (e.getKey() == Side.TOP) {
					this.top = p;
				} else {
					this.bottom = p;
				}
			}
		}

		this.matchId = match.getId();
		this.winner = game.getWinner();
		this.arcade = game.getArcade();
		this.winCondition = winCondition;
		this.seed = game.getSeed();
	}

	public int getMatchId() {
		return matchId;
	}

	public HistoryPlayer getTop() {
		return top;
	}

	public HistoryPlayer getBottom() {
		return bottom;
	}

	public Side getWinner() {
		return winner;
	}

	public HistoryPlayer getWinnerPlayer() {
		if (winner == null) return null;

		return switch (winner) {
			case TOP -> top;
			case BOTTOM -> bottom;
		};
	}

	public Arcade getArcade() {
		return arcade;
	}

	public String getWinCondition() {
		return winCondition;
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public long getSeed() {
		return seed;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		HistoryInfo that = (HistoryInfo) o;
		return matchId == that.matchId;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(matchId);
	}
}
