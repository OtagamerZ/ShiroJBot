package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.model.RelayBlockList;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import static com.kuuhaku.model.RelayBlockList.unblock;

public class UnblockEvent implements Job {
	public static JobDetail unblock;

	@Override
	public void execute(JobExecutionContext context) {
		RelayBlockList.getBlockedIDs().forEach(id -> {
			if (!id.isEmpty()) {
				Main.getJibril().getUserById(id).openPrivateChannel().queue(c -> c.sendMessage(":stopwatch: | O tempo do seu bloqueio acabou, você está liberado para conversar no chat global novamente.\n\nReincidências podem fazer com que seja bloqueado permanentemente do chat global.").queue());
			}
			unblock(id);
		});
	}
}
