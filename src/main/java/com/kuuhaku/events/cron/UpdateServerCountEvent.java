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
import com.kuuhaku.utils.Helper;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.IOException;

public class UpdateServerCountEvent implements Job {
	public static JobDetail updateServerCount;

	@Override
	public void execute(JobExecutionContext context) {
		int size = Main.getInfo().getAPI().getGuilds().size();
		Main.getInfo().getDblApi().setStats(size);
		if (System.getenv().containsKey("DBL_TOKEN")) try {
			JSONObject jo = new JSONObject();

			jo.put("shardCount", 1);
			jo.put("guildCount", size);

			String response = Helper.post("https://discord.bots.gg/api/v1/bots/" + Main.getInfo().getAPI().getSelfUser().getId() + "/stats", jo, System.getenv("DBL_TOKEN")).toString();
			Helper.logger(this.getClass()).info(response);
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
