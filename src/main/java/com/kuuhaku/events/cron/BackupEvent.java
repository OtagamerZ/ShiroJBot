package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class BackupEvent implements Job {
	public static JobDetail backup;

	@Override
	public void execute(JobExecutionContext context) {
		Main.getInfo().getAPI().getPresence().setActivity(Main.getRandomActivity());
		MySQL.dumpData(new DataDump(SQLite.getCADump(), SQLite.getGuildDump()));
		Helper.log(this.getClass(), LogLevel.INFO, "Respostas/Guilds salvos com sucesso!");
		MySQL.dumpData(new DataDump(SQLite.getMemberDump()));
		Helper.log(this.getClass(), LogLevel.INFO, "Membros salvos com sucesso!");
	}
}
