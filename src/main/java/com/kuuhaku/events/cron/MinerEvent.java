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
import com.kuuhaku.controller.postgresql.GuildBuffDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildBuff;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ServerBuff;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.List;
import java.util.stream.Collectors;

public class MinerEvent implements Job {
	public static JobDetail miner;

	@Override
	public void execute(JobExecutionContext context) {
		List<GuildBuff> gbs = GuildBuffDAO.getAllBuffs().stream().filter(gb -> gb.getBuffs().contains(new ServerBuff(1, ServerBuff.MINER_TIER_1))).collect(Collectors.toList());

		for (GuildBuff gb : gbs) {
			GuildConfig gc = GuildDAO.getGuildById(gb.getId());
			Guild g = Main.getInfo().getGuildByID(gc.getGuildID());

			if (gc.getCanalKawaipon() != null && !gc.getCanalKawaipon().isBlank()) {
				TextChannel chn = g.getTextChannelById(gc.getCanalKawaipon());
				if (chn != null)
					Helper.spawnKawaipon(gc, chn);
				else {
					gc.setCanalKawaipon(null);
					GuildDAO.updateGuildSettings(gc);
					if (g.getDefaultChannel() != null)
						Helper.spawnKawaipon(gc, g.getDefaultChannel());
				}
			} else if (g.getDefaultChannel() != null)
				Helper.spawnKawaipon(gc, g.getDefaultChannel());

			if (gc.getCanalDrop() != null && !gc.getCanalDrop().isBlank()) {
				TextChannel chn = g.getTextChannelById(gc.getCanalDrop());
				if (chn != null)
					Helper.spawnDrop(gc, chn);
				else {
					gc.setCanalDrop(null);
					GuildDAO.updateGuildSettings(gc);
					if (g.getDefaultChannel() != null)
						Helper.spawnDrop(gc, g.getDefaultChannel());
				}
			} else if (g.getDefaultChannel() != null)
				Helper.spawnDrop(gc, g.getDefaultChannel());
		}
	}
}
