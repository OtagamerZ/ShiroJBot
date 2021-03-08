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
import com.kuuhaku.model.common.RelayBlockList;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Emote;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class HourlyEvent implements Job {
	public static JobDetail hourly;

	@Override
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void execute(JobExecutionContext context) {
		if (!Main.getInfo().isLive()) Main.getShiroShards().setActivity(Main.getRandomActivity());

		com.kuuhaku.controller.postgresql.BackupDAO.dumpData(
				new DataDump(
						BackupDAO.getCADump(),
						BackupDAO.getMemberDump(),
						BackupDAO.getGuildDump(),
						BackupDAO.getPoliticalStateDump()
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

		if (LocalDateTime.now().getHour() % 6 == 0) {

			if (LocalDateTime.now().getHour() % 12 == 0) {
				RelayBlockList.clearBlockedThumbs();
				RelayBlockList.refresh();

			/*GuildDAO.getAllGuilds().forEach(gc -> {
				if (gc.getCargoVip() != null && !gc.getCargoVip().isEmpty()) {
					Guild g = Main.getInfo().getGuildByID(gc.getGuildID());
					if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) return;
					Role r = g.getRoleById(GuildDAO.getGuildById(g.getId()).getCargoVip());
					assert r != null;
					g.retrieveInvites().complete().stream()
							.filter(inv -> inv.getInviter() != null && inv.getInviter() != Objects.requireNonNull(g.getOwner()).getUser() && !inv.getInviter().isFake() && !inv.getInviter().isBot())
							.map(inv -> {
								Member m = g.getMember(inv.getInviter());
								assert m != null;
								if (inv.getUses() / TimeUnit.DAYS.convert(m.getTimeJoined().toEpochSecond(), TimeUnit.SECONDS) > 1)
									return g.addRoleToMember(m, r);
								else return g.removeRoleFromMember(m, r);
							}).collect(Collectors.toList())
							.forEach(rst -> {
								try {
									rst.complete();
								} catch (NullPointerException ignore) {
								}
							});
				}
			});*/
			}
		}
	}
}
