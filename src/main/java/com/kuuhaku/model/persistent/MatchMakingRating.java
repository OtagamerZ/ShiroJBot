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

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.persistence.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Entity
@Table(name = "matchmakingrating")
public class MatchMakingRating {
	@Id
	private String userId;

	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private long mmr = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int rankPoints = 0;

	@Enumerated(value = EnumType.STRING)
	private RankedTier tier = RankedTier.UNRANKED;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int promLosses = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int promWins = 0;

	@Temporal(TemporalType.TIMESTAMP)
	private Date blockedUntil = null;

	public MatchMakingRating(String userId) {
		this.userId = userId;
	}

	public MatchMakingRating() {
	}

	public String getUserId() {
		return userId;
	}

	public User getUser() {
		return Main.getInfo().getUserByID(userId);
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getMMR() {
		return mmr;
	}

	public void addMMR(long gained, long opponent) {
		this.mmr += gained * (this.mmr == 0 ? 1 : Helper.prcnt(opponent, this.mmr));
	}

	public void removeMMR(long lost, long opponent) {
		this.mmr -= Math.min(this.mmr, lost * (opponent == 0 ? 1 : Helper.prcnt(this.mmr, opponent)));
	}

	public void setMMR(long mmr) {
		this.mmr = mmr;
	}

	public int getRankPoints() {
		return rankPoints;
	}

	public void increaseRankPoints() {
		double mmrModif = Helper.prcnt(mmr, MatchMakingRatingDAO.getAverageMMR(tier));
		int rpValue = (int) Math.round(mmrModif * 15);
		if (tier == RankedTier.UNRANKED) {
			promWins++;

			if (promWins + promLosses == tier.getMd()) {
				tier = RankedTier.MINARAI_IV;
				rankPoints = 0;
				promWins = promLosses = 0;
				return;
			}
			return;
		} else if (rankPoints == 100) {
			promWins++;

			if (promWins > tier.getMd() / 2f) {
				tier = tier.getNext();
				rankPoints = 0;
				promWins = promLosses = 0;
				return;
			}
			return;
		}

		if (tier != RankedTier.DAI_MAJUTSU_SHI)
			rankPoints += Math.min(100 - rankPoints, rpValue);
		else
			rankPoints += rpValue;
	}

	public void decreaseRankPoints() {
		double mmrModif = Helper.prcnt(MatchMakingRatingDAO.getAverageMMR(tier), mmr);
		int rpValue = (int) Math.round(mmrModif * 15);

		if (tier == RankedTier.UNRANKED) {
			promLosses++;

			if (promWins + promLosses == tier.getMd()) {
				tier = RankedTier.MINARAI_IV;
				rankPoints = 0;
				promWins = promLosses = 0;
				return;
			}
			return;
		} else if (rankPoints == 100) {
			promLosses++;

			if (promLosses > tier.getMd() / 2f) {
				rankPoints -= rpValue * promLosses;
				promWins = promLosses = 0;
				return;
			}
			return;
		}

		if (rankPoints == 0 && Helper.chance(20 * mmrModif) && tier != RankedTier.MINARAI_IV) {
			tier = tier.getPrevious();
			rankPoints = 75;
			return;
		}

		rankPoints -= Math.max(rpValue, rankPoints);
	}

	public RankedTier getTier() {
		return tier;
	}

	public int getPromLosses() {
		return promLosses;
	}

	public int getPromWins() {
		return promWins;
	}

	public boolean isBlocked() {
		if (blockedUntil == null) return false;
		else if (Date.from(Instant.now(Clock.system(ZoneId.of("GMT-3")))).after(blockedUntil)) {
			blockedUntil = null;
			return false;
		} else return true;
	}

	public void block(int time, TemporalUnit unit) {
		blockedUntil = Date.from(Instant.now(Clock.system(ZoneId.of("GMT-3"))).plus(time, unit));
	}

	public static Map<Side, Pair<String, Map<String, Integer>>> calcMMR(MatchHistory mh) {
		Map<Side, Pair<String, Map<String, Integer>>> finalData = new HashMap<>();
		for (Side s : Side.values()) {
			List<MatchRound> rounds = mh.getRounds().entrySet().stream()
					.sorted(Comparator.comparingInt(Map.Entry::getKey))
					.map(Map.Entry::getValue)
					.filter(mr -> mr.getSide() == s)
					.collect(Collectors.toList());
			Pair<String, Map<String, Integer>> fd = Pair.of(
					rounds.get(0)
							.getScript()
							.getJSONObject(s.name().toLowerCase())
							.getString("id"),
					new HashMap<>()
			);

			AtomicReference<JSONObject> ph = new AtomicReference<>();
			for (MatchRound round : rounds) {
				JSONObject jo = round.getScript().getJSONObject(s.name().toLowerCase());
				if (ph.get() == null)
					jo.toMap().forEach((k, v) -> {
						if (!k.equals("id")) {
							int rv = (int) v;
							fd.getRight().put(k, rv);
						}
					});
				else
					jo.toMap().forEach((k, v) -> {
						if (!k.equals("id")) {
							int rv = (int) v - ph.get().optInt(k);
							fd.getRight().computeIfPresent(k, (key, value) -> rv);
						}
					});
				ph.set(jo);
			}
			finalData.put(s, fd);
		}

		return finalData;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MatchMakingRating that = (MatchMakingRating) o;
		return Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId);
	}
}
