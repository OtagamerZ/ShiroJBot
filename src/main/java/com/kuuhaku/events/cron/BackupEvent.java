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
import com.kuuhaku.controller.Sweeper;
import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.utils.Helper;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class BackupEvent implements Job {
	public static JobDetail backup;

	@Override
	public void execute(JobExecutionContext context) {
		Main.getInfo().getAPI().getPresence().setActivity(Main.getRandomActivity());

		Helper.logger(this.getClass()).info(Sweeper.mark() + " entradas dispensáveis encontradas!");

		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getCADump(), com.kuuhaku.controller.sqlite.BackupDAO.getGuildDump(), com.kuuhaku.controller.sqlite.BackupDAO.getAppUserDump(), com.kuuhaku.controller.sqlite.BackupDAO.getKawaigotchiDump(), com.kuuhaku.controller.sqlite.BackupDAO.getPoliticalStateDump()));
		Helper.logger(this.getClass()).info("Respostas/Guilds/Usuários/Kawaigotchis/Exceeds salvos com sucesso!");
		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getMemberDump()));
		Helper.logger(this.getClass()).info("Membros salvos com sucesso!");

		Sweeper.sweep();

		System.gc();
	}
}
