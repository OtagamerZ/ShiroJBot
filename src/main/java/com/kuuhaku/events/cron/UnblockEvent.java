package com.kuuhaku.events.cron;

import com.kuuhaku.model.RelayBlockList;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class UnblockEvent implements Job {
	public static JobDetail unblock;

	@Override
	public void execute(JobExecutionContext context) {
		RelayBlockList.clearBlockedThumbs();
		RelayBlockList.refresh();
	}
}
