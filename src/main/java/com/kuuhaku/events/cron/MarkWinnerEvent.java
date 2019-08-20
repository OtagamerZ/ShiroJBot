package com.kuuhaku.events.cron;

import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Exceed;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MarkWinnerEvent implements Job {
	public static JobDetail markWinner;

	@Override
	public void execute(JobExecutionContext context) {
		List<Exceed> exceeds = new ArrayList<>();
		for (ExceedEnums ex : ExceedEnums.values()) {
			exceeds.add(MySQL.getExceed(ex));
		}
		exceeds.sort(Comparator.comparingLong(Exceed::getExp).reversed());

		MySQL.markWinner(exceeds.get(0).getExceed());
		Helper.log(this.getClass(), LogLevel.INFO, "Vencedor mensal: " + exceeds.get(0).getExceed().getName());
	}
}
