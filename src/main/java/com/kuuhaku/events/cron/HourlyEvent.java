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

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.MatchDAO;
import com.kuuhaku.controller.postgresql.StockMarketDAO;
import com.kuuhaku.controller.sqlite.BackupDAO;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Emote;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.util.List;

public class HourlyEvent implements Job {
	public static JobDetail hourly;

	@Override
	public void execute(JobExecutionContext context) {
		if (!Main.getInfo().isLive()) Main.getShiroShards().setActivity(Main.getRandomActivity());

		com.kuuhaku.controller.postgresql.BackupDAO.dumpData(
				new DataDump(
						BackupDAO.getCADump(),
						BackupDAO.getMemberDump(),
						BackupDAO.getGuildDump(),
						BackupDAO.getKawaigotchiDump(),
						BackupDAO.getPoliticalStateDump(),
						null
				), false
		);

		Main.getInfo().setWinner(ExceedDAO.getWinner());
		Helper.logger(this.getClass()).info("Atualizado vencedor mensal.");

		if (Main.getInfo().getDblApi() != null) {
			int size = Main.getShiroShards().getGuilds().size();
			Main.getInfo().getDblApi().setStats(size);
			if (System.getenv().containsKey("DBL_TOKEN")) {
				JSONObject jo = new JSONObject();

				jo.put("guildCount", size);

				String response = Helper.post("https://discord.bots.gg/api/v1/bots/" + Main.getShiroShards().getShards().get(0).getSelfUser().getId() + "/stats", jo, System.getenv("DBL_TOKEN")).toString();
				Helper.logger(this.getClass()).debug(response);
			}
		}

		ShiroInfo.getEmoteCache().clear();
		for (Emote emote : Main.getShiroShards().getEmotes()) {
			ShiroInfo.getEmoteCache().put(":" + emote.getName() + ":", emote.getId());
		}

		System.gc();

		for (File file : Main.getInfo().getCollectionsFolder().listFiles()) {
			file.delete();
		}

		List<Account> accs = AccountDAO.getVolatileAccounts();
		for (Account acc : accs) {
			acc.expireVCredit();
			AccountDAO.saveAccount(acc);
		}

		MatchDAO.cleanHistory();
		StockMarketDAO.removeZeroInvestments();
	}
}
