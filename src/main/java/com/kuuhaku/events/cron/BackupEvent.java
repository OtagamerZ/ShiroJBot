/*
 * This file is part of Shiro J Bot.
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
import com.kuuhaku.controller.mysql.BackupDAO;
import com.kuuhaku.controller.mysql.CampaignDAO;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Activity;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class BackupEvent implements Job {
	public static JobDetail backup;

	@Override
	public void execute(JobExecutionContext context) {
		Main.getInfo().getAPI().getPresence().setActivity(Main.getRandomActivity());
		Main.getTet().getPresence().setActivity(Activity.playing(" em diversos mundos espalhados em " + Main.getTet().getGuilds().size() + " servidores!"));

		Helper.logger(this.getClass()).info(Sweeper.mark() + " entradas dispensáveis encontradas!");

		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getCADump(), com.kuuhaku.controller.sqlite.BackupDAO.getGuildDump(), com.kuuhaku.controller.sqlite.BackupDAO.getAppUserDump(), com.kuuhaku.controller.sqlite.BackupDAO.getKawaigotchiDump()));
		Helper.logger(this.getClass()).info("Respostas/Guilds/Usuários/Kawaigotchis salvos com sucesso!");
		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getMemberDump()));
		Helper.logger(this.getClass()).info("Membros salvos com sucesso!");
		CampaignDAO.saveCampaigns(Main.getInfo().getGames());
		Helper.logger(this.getClass()).info("Campanhas salvas com sucesso!");

		Sweeper.sweep();
	}
}
