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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.helpers.HttpHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class HourlyEvent implements Job {

	@Override
	public void execute(JobExecutionContext context) {
		for (JDA shard : Main.getShiro().getShards()) {
			shard.getPresence().setActivity(Main.getRandomActivity());
		}

		if (Main.getInfo().getTopggClient() != null) {
			int size = Main.getShiro().getGuilds().size();
			Main.getInfo().getTopggClient().setStats(size);
			if (System.getenv().containsKey("DBL_TOKEN")) {
				JSONObject jo = new JSONObject();

				jo.put("guildCount", size);

				String response = HttpHelper.post("https://discord.bots.gg/api/v1/bots/" + Main.getShiro().getShards().get(0).getSelfUser().getId() + "/stats", jo, System.getenv("DBL_TOKEN")).toString();
				MiscHelper.logger(this.getClass()).debug(response);
			}
		}

		Main.getEmoteCache().clear();
		for (Emote emote : Main.getShiro().getEmotes()) {
			Main.getEmoteCache().put(":" + emote.getName() + ":", emote.getId());
		}

		System.runFinalization();

		File[] files = Main.getInfo().getCollectionsFolder().listFiles();
		if (files != null) {
			for (File file : files) {
				if (!file.delete()) {
					MiscHelper.logger(this.getClass()).warn("Failed to delete file at " + file.toPath().getFileName());
				}
			}
		}

		List<Account> accs = Account.queryAll(Account.class, "SELECT a FROM Account a WHERE a.vBalance > 0");
		for (Account acc : accs) {
			acc.expireVCredit();
			acc.save();
		}

		MatchDAO.cleanHistory();

		LocalDateTime time = LocalDateTime.now();
		List<Hero> heroes = KawaiponDAO.getHeroes();
		for (Hero hero : heroes) {
			if (hero.isQuesting()) continue;

			if (hero.isResting()) {
				hero.rest();
			}

			if (time.getHour() % 6 == 0) hero.setEnergy(hero.getMaxEnergy());
			KawaiponDAO.saveHero(hero);
		}
	}
}
