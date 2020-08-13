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
import com.kuuhaku.controller.sqlite.BackupDAO;
import com.kuuhaku.model.common.DataDump;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class BackupEvent implements Job {
	public static JobDetail backup;

	@Override
	public void execute(JobExecutionContext context) {
		if (!Main.getInfo().isLive()) Main.getInfo().getAPI().getPresence().setActivity(Main.getRandomActivity());

		com.kuuhaku.controller.postgresql.BackupDAO.dumpData(
				new DataDump(
						BackupDAO.getCADump(),
						BackupDAO.getMemberDump(),
						BackupDAO.getGuildDump(),
						BackupDAO.getKawaigotchiDump(),
						BackupDAO.getPoliticalStateDump(),
						null
				)
		);

		System.gc();
	}
}
