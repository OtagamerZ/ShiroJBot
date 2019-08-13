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

import javax.persistence.NoResultException;

public class PartnerCheckEvent implements Job {
	public static JobDetail check;

	@Override
	public void execute(JobExecutionContext context) {
		Main.getJibril().getGuilds().forEach(g -> {
			try {
				if (!MySQL.getTagById(g.getOwnerId()).isPartner()) {
					g.leave().queue();
					Helper.log(this.getClass(), LogLevel.INFO, "Saí do servidor " + g.getName() + " por " + g.getOwner().getUser().getAsTag() + " não estar na lista de parceiros.");
				}
			} catch (NoResultException e) {
				Helper.log(this.getClass(), LogLevel.WARN, "Erro ao sair de " + g.getName() + ", gerenciado por " + g.getOwner().getUser().getAsTag() + "(" + g.getOwnerId() + ").");
			}
		});
	}
}
