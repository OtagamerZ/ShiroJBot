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

package com.kuuhaku.model.persistent;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.model.records.MatchInfo;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "matchhistory")
public class MatchHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(value = EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	@JoinColumn(nullable = false, name = "matchhistory_id")
	private Map<String, Side> players = new HashMap<>();

	@Enumerated(value = EnumType.STRING)
	private Side winner = null;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean ranked = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean wo = false;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "matchhistory_id")
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

	public List<MatchRound> getRounds(Side s) {
		return rounds.entrySet().stream()
				.sorted(Comparator.comparingInt(Map.Entry::getKey))
				.map(Map.Entry::getValue)
				.filter(r -> r.getSide() == s)
				.toList();
	}

	public List<MatchRound> getRounds(Side s, String uid) {
		return rounds.entrySet().stream()
				.sorted(Comparator.comparingInt(Map.Entry::getKey))
				.map(Map.Entry::getValue)
				.filter(r -> r.getSide() == s && r.getUid().equals(uid))
				.toList();
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

	public Map<String, MatchInfo> getStats() {
		Map<String, MatchInfo> out = new HashMap<>();

		for (Side s : Side.values()) {
			Set<String> players = this.players.entrySet().stream()
					.filter(e -> e.getValue() == s)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());

			for (String uid : players) {
				List<MatchRound> yours = getRounds(s, uid);
				List<MatchRound> his = getRounds(s.getOther());

				double baseMana = yours.stream()
						.mapToInt(MatchRound::getBaseMp)
						.average()
						.orElse(0);
				int spentMana = yours.stream()
						.mapToInt(r -> r.getBaseMp() - r.getMp())
						.sum();

				double baseHp = yours.stream()
						.mapToInt(MatchRound::getBaseHp)
						.average()
						.orElse(0);
				int damageSustained = yours.stream()
						.mapToInt(r -> r.getBaseHp() - r.getHp())
						.sum();

				double baseOpHp = his.stream()
						.mapToInt(MatchRound::getBaseHp)
						.average()
						.orElse(0);
				int damageDealt = his.stream()
						.mapToInt(r -> r.getBaseHp() - r.getHp())
						.sum();

				int summons = (int) yours.stream()
						.flatMap(r -> Stream.of(r.getChampions(), r.getEvogears()))
						.flatMap(List::stream)
						.distinct()
						.count();

				double manaEff = 0;
				if (spentMana != 0) {
					manaEff = (baseMana + summons) / spentMana;
				}

				double damageEff = (double) damageDealt / yours.size();
				double expEff = baseOpHp / yours.size();
				double sustainEff = 1 - damageSustained / baseHp;

				out.put(uid, new MatchInfo(
						uid,
						s,
						winner == s,
						manaEff,
						damageEff / expEff,
						sustainEff
				));
			}
		}

		return out;
	}
}
