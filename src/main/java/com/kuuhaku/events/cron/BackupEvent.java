package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.BackupDAO;
import com.kuuhaku.controller.Sweeper;
import com.kuuhaku.model.DataDump;
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

		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getCADump(), com.kuuhaku.controller.sqlite.BackupDAO.getGuildDump()));
		Helper.logger(this.getClass()).info("Respostas/Guilds salvos com sucesso!");
		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getMemberDump()));
		Helper.logger(this.getClass()).info("Membros salvos com sucesso!");

		Sweeper.sweep();
	}
}
