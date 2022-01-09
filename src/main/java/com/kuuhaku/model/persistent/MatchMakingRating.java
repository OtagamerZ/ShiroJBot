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

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.controller.postgresql.DynamicParameterDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

@Entity
@Table(name = "matchmakingrating")
public class MatchMakingRating {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

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

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int wins = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int losses = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int banked = 0;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime blockedUntil = null;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int joins = 0;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int evades = 0;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String master = "";

	public MatchMakingRating(String uid) {
		this.uid = uid;
	}

	public MatchMakingRating() {
	}

	public String getUid() {
		return uid;
	}

	public User getUser() {
		return Main.getInfo().getUserByID(uid);
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public long getMMR() {
		return mmr;
	}

	public void addMMR(long gained, long opponent, boolean ranked) {
		double score = gained * (this.mmr == 0 ? 1 : Helper.prcnt(opponent, this.mmr)) * (ranked ? 1 : 0.5);
		this.mmr += score;

		ClanMember cm = ClanDAO.getClanMember(uid);
		if (cm != null) {
			cm.addScore((long) score);
			ClanDAO.saveMember(cm);
		}
	}

	public void removeMMR(long lost, long opponent, boolean ranked) {
		double score = Math.min(this.mmr, lost * (opponent == 0 ? 1 : Helper.prcnt(this.mmr, opponent))) * (ranked ? 1 : 0.5);
		this.mmr = (long) Math.max(0, mmr - score);

		ClanMember cm = ClanDAO.getClanMember(uid);
		if (cm != null) {
			cm.removeScore((long) score);
			ClanDAO.saveMember(cm);
		}
	}

	public void setMMR(long mmr) {
		this.mmr = mmr;
	}

	public int getRankPoints() {
		return rankPoints;
	}

	public void increaseRankPoints(long opMMR) {
		if (tier.getTier() >= RankedTier.ADEPT_IV.getTier())
			banked = Math.min(banked + 7 - (tier.getTier() - 4), 28);
		double mmrModif = Helper.prcnt(mmr, Helper.average((1250 * tier.ordinal()), MatchMakingRatingDAO.getAverageMMR(tier))) * Helper.prcnt((double) opMMR, mmr);
		int rpValue = Helper.clamp((int) Math.round(mmrModif * 15), 5, 30);

		if (tier == RankedTier.UNRANKED) {
			promWins++;

			if (promWins + promLosses == tier.getMd()) {
				tier = tier.getNext();
				rankPoints = (int) (50 * Helper.prcnt(promWins, tier.getMd()));
				promWins = promLosses = 0;

				if (this.master.isBlank()) this.master = "none";
				Main.getInfo().getUserByID(uid).openPrivateChannel()
						.flatMap(c -> c.sendMessage("Parabéns, você foi promovido para o tier %s (%s)".formatted(tier.getTier(), tier.getName())))
						.queue(null, Helper::doNothing);
				return;
			}
			return;
		} else if (rankPoints == tier.getPromRP()) {
			promWins++;

			if (promWins > tier.getMd() / 2f) {
				tier = tier.getNext();
				rankPoints = rpValue;
				promWins = promLosses = 0;

				if (StringUtils.isNumeric(master) && tier.getTier() > 1) {
					Account acc = AccountDAO.getAccount(master);
					master = "FULFILLED_" + master;
					User u = Main.getInfo().getUserByID(uid);
					u.openPrivateChannel()
							.flatMap(c -> c.sendMessage("Parabéns, você foi promovido para o tier %s (%s), além de receber **5 sínteses gratuitas** no comando `sintetizar`.".formatted(tier.getTier(), tier.getName())))
							.flatMap(c -> Main.getInfo().getUserByID(master).openPrivateChannel())
							.flatMap(c -> c.sendMessage("Seu discípulo " + u.getAsTag() + " alcançou o ranking de " + tier.getName() + ", você recebeu **50.000 CR**!"))
							.queue(null, Helper::doNothing);

					acc.addCredit(30000, this.getClass());
					AccountDAO.saveAccount(acc);

					DynamicParameter freeRolls = DynamicParameterDAO.getParam("freeSynth_" + uid);
					DynamicParameterDAO.setParam("freeSynth_" + uid, String.valueOf(NumberUtils.toInt(freeRolls.getValue()) + 5));
				} else {
					Main.getInfo().getUserByID(uid).openPrivateChannel()
							.flatMap(c -> c.sendMessage("Parabéns, você foi promovido para o tier %s (%s)!".formatted(tier.getTier(), tier.getName())))
							.queue(null, Helper::doNothing);
				}

				return;
			}
			return;
		}

		if (tier != RankedTier.ARCHMAGE)
			rankPoints = Math.min(rankPoints + rpValue, tier.getPromRP());
		else
			rankPoints += rpValue;
	}

	public void decreaseRankPoints(long opMMR) {
		if (tier.getTier() >= RankedTier.ADEPT_IV.getTier())
			banked = Math.min(banked + 7 - (tier.getTier() - 4), 28);
		double mmrModif = Helper.prcnt(Helper.average((1250 * tier.ordinal()), MatchMakingRatingDAO.getAverageMMR(tier)), mmr) * Helper.prcnt(mmr, (double) opMMR);
		int rpValue = Helper.clamp((int) Math.round(mmrModif * 15), 5, 30);

		if (tier == RankedTier.UNRANKED) {
			promLosses++;

			if (promWins + promLosses == tier.getMd()) {
				tier = tier.getNext();
				rankPoints = (int) (50 * Helper.prcnt(promWins, tier.getMd()));
				promWins = promLosses = 0;

				if (this.master.isBlank()) this.master = "none";
				Main.getInfo().getUserByID(uid).openPrivateChannel()
						.flatMap(c -> c.sendMessage("Parabéns, você foi promovido para o tier %s (%s)!".formatted(tier.getTier(), tier.getName())))
						.queue(null, Helper::doNothing);
			}
			return;
		} else if (rankPoints == tier.getPromRP()) {
			promLosses++;

			if (promLosses > tier.getMd() / 2f) {
				rankPoints -= rpValue;
				promWins = promLosses = 0;
				return;
			}
			return;
		}

		if (rankPoints == 0 && Helper.chance(20 * mmrModif) && tier != RankedTier.INITIATE_IV) {
			tier = tier.getPrevious();
			rankPoints = Math.max(0, rankPoints - rpValue);
			Main.getInfo().getUserByID(uid).openPrivateChannel()
					.flatMap(c -> c.sendMessage("Você foi rebaixado para o tier %s (%s).".formatted(tier.getTier(), tier.getName())))
					.queue(null, Helper::doNothing);
			return;
		}

		rankPoints = Math.max(0, rankPoints - rpValue);
	}

	public void applyInactivityPenalty() {
		if (banked > 0) {
			banked--;
			return;
		}

		rankPoints = Math.max(0, rankPoints - 25 * (tier.getTier() - 3));
		if (rankPoints == 0) {
			tier = tier.getPrevious();
			rankPoints = 75;
			Main.getInfo().getUserByID(uid).openPrivateChannel()
					.flatMap(c -> c.sendMessage("Você foi rebaixado para o tier %s (%s) por inatividade.".formatted(tier.getTier(), tier.getName())))
					.queue(null, Helper::doNothing);
		}
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

	public int getWins() {
		return wins;
	}

	public void addWin() {
		wins++;
	}

	public int getLosses() {
		return losses;
	}

	public void addLoss() {
		losses++;
	}

	public int getBanked() {
		return banked;
	}

	public String getWinrate() {
		if (losses == 0) return "perfeito";
		return "%s%% (V/D)".formatted(Helper.prcntToInt((float) wins, wins + losses));
	}

	public boolean isBlocked() {
		if (blockedUntil == null) return false;
		else if (ZonedDateTime.now(ZoneId.of("GMT-3")).isAfter(blockedUntil)) {
			blockedUntil = null;
			MatchMakingRatingDAO.saveMMR(this);
			return false;
		} else return true;
	}

	public long getRemainingBlock() {
		if (blockedUntil == null) return 0;

		ZonedDateTime today = ZonedDateTime.now(ZoneId.of("GMT-3"));

		return Math.max(0, blockedUntil.toInstant().toEpochMilli() - today.toInstant().toEpochMilli());
	}

	public void block(int time, TemporalUnit unit) {
		blockedUntil = ZonedDateTime.now(ZoneId.of("GMT-3")).plus(time, unit);
	}

	public int getJoins() {
		return joins;
	}

	public void setJoins(int joins) {
		this.joins = joins;
	}

	public int getEvades() {
		return evades;
	}

	public void setEvades(int evades) {
		this.evades = evades;
	}

	public String getMaster() {
		return master;
	}

	public void setMaster(String master) {
		this.master = master;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MatchMakingRating that = (MatchMakingRating) o;
		return Objects.equals(uid, that.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid);
	}
}
