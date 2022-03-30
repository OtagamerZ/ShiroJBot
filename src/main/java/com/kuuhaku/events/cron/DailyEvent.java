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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.User;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Calendar;
import java.util.List;

public class DailyEvent implements Job {

	@Override
	public void execute(JobExecutionContext context) {
		Calendar c = Calendar.getInstance();
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			Account.apply("""
					UPDATE Account a
					SET a.vBalance = a.vBalance + (a.balance / 2)
					  , a.balance  = a.balance / 2
					  , a.spent = 0
					WHERE a.balance > 100000
					  AND (a.spent * 1.0) / (a.spent + a.balance) < 0.1
					""");
			Account.apply("UPDATE Account a SET a.weeklyRolls = 3 WHERE a.weeklyRolls < 3");
		}

		if (c.get(Calendar.DAY_OF_MONTH) == 1) {
			List<Clan> unpaid = ClanDAO.getUnpaidClans();
			for (Clan clan : unpaid) {
				if (clan.getVault() < clan.getTier().getRent()) {
					User u = Main.getUserByID(clan.getLeader().getUid());

					u.openPrivateChannel()
							.flatMap(s -> s.sendMessage(":warning: | Alerta: Não há saldo suficiente no cofre do clã " + clan.getName() + " para pagamento do aluguel. Por favor deposite " + StringHelper.separate(clan.getTier().getRent()) + " CR no cofre do clã.\n**Você tem até dia 8 ou o clã será desfeito.**"))
							.queue(null, MiscHelper::doNothing);
				} else {
					clan.payRent();
					ClanDAO.saveClan(clan);
				}
			}
		} else if (c.get(Calendar.DAY_OF_MONTH) == 8) {
			List<Clan> unpaid = ClanDAO.getUnpaidClans();
			for (Clan clan : unpaid) {
				if (clan.getVault() < clan.getTier().getRent()) {
					ClanDAO.removeClan(clan);
				} else {
					clan.payRent();
					ClanDAO.saveClan(clan);
				}
			}
		}

		if (c.get(Calendar.MONTH) == Calendar.JANUARY && c.get(Calendar.DAY_OF_MONTH) == 11) {
			MatchMakingRatingDAO.resetRanks();
		} else if (c.get(Calendar.MONTH) == Calendar.DECEMBER && c.get(Calendar.DAY_OF_MONTH) == 21) {
			List<MatchMakingRating> mmrs = MatchMakingRatingDAO.getMMRRank();
			for (MatchMakingRating mmr : mmrs) {
				if (mmr.getTier() == RankedTier.UNRANKED) continue;
				int fac = (int) Math.pow(1.8, mmr.getTier().getTier() - 1);
				int credits = 10000 * fac;

				Account acc = Account.find(Account.class, mmr.getUid());
				acc.addCredit(credits, this.getClass());
				if (mmr.getTier().getTier() >= 5)
					acc.addGem(3 * (mmr.getTier().getTier() - 4));
				acc.save();

				Main.getUserByID(mmr.getUid()).openPrivateChannel()
						.flatMap(ch -> ch.sendMessage("Parabéns por alcançar o ranking **%s** nesta temporada, como recompensa você recebeu **%s CR**. GG WP!".formatted(mmr.getTier().getName(), StringHelper.separate(credits))))
						.queue(null, MiscHelper::doNothing);
			}
		} else {
			List<MatchMakingRating> mmrs = MatchMakingRatingDAO.getMMRRank().stream()
					.filter(mmr -> mmr.getTier().getTier() >= RankedTier.ADEPT_IV.getTier())
					.toList();

			for (MatchMakingRating mmr : mmrs) {
				mmr.applyInactivityPenalty();

				MatchMakingRatingDAO.saveMMR(mmr);
			}
		}
	}
}
