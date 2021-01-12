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

package com.kuuhaku.events.cron;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.MatchMakingRating;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.Calendar;
import java.util.List;

public class DailyEvent implements Job {
	public static JobDetail daily;

	@Override
	public void execute(JobExecutionContext context) {
		Calendar c = Calendar.getInstance();
		if (c.get(Calendar.MONTH) == Calendar.JANUARY && c.get(Calendar.DAY_OF_MONTH) == 11) {
			List<MatchMakingRating> mmrs = MatchMakingRatingDAO.getMMRRank();
			for (MatchMakingRating mmr : mmrs) {
				Account acc = AccountDAO.getAccount(mmr.getUserId());
				acc.addCredit(10000 * mmr.getTier().getTier(), this.getClass());
				AccountDAO.saveAccount(acc);
			}
			MatchMakingRatingDAO.resetRanks();
		}
	}
}
