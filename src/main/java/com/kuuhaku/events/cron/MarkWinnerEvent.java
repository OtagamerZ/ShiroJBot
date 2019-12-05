package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL.Exceed;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class MarkWinnerEvent implements Job {
	public static JobDetail markWinner;

	@Override
	public void execute(JobExecutionContext context) {
		Exceed.markWinner(Exceed.findWinner());
		Helper.logger(this.getClass()).info("Vencedor mensal: " + Exceed.getWinner());

		String ex = Exceed.getWinner();
		Exceed.getExceedMembers(ExceedEnums.getByName(ex)).forEach(em ->
				Main.getInfo().getUserByID(em.getMid()).openPrivateChannel().queue(c -> {
					try {
						c.sendMessage("O seu exceed foi campeão neste mês, parabéns!\n" +
								"Todos da " + ex + " ganharão experiência em dobro durante 1 semana.").queue();
					} catch (Exception ignore) {
					}
				}));
	}
}
