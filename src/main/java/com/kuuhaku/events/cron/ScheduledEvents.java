/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.model.records.CronJob;
import com.kuuhaku.utils.helpers.MiscHelper;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduledEvents implements JobListener {
	private static final ExecutorService exec = Executors.newSingleThreadExecutor();
	private static ScheduledEvents self = null;
	private final Scheduler sched;

	public ScheduledEvents() {
		Thread.currentThread().setName("crontab");
		Scheduler s = null;
		try {
			s = new StdSchedulerFactory().getScheduler();
			schedule(
					new CronJob("10 seconds", "0/10 * * ? * * *"),
					new CronJob("1 minute", "0 0/1 * ? * * *"),
					new CronJob("10 minutes", "0 0/10 * ? * * *"),
					new CronJob("1 hour", "0 0 0/1 ? * * *"),
					new CronJob("1 day", "0 0 0 ? * * *"),
					new CronJob("1 month", "0 0 0 1 * ? *")
			);
		} catch (SchedulerException e) {
			MiscHelper.logger(this.getClass()).error("Failed to initialize cron scheduler: " + e);
		} finally {
			sched = s;
		}
	}

	public static void init() {
		exec.execute(() -> self = new ScheduledEvents());
	}

	private void schedule(CronJob... jobs) throws SchedulerException {
		sched.clear();

		for (CronJob job : jobs) {
			JobDetail detail = JobBuilder.newJob(TenthSecondEvent.class)
					.withIdentity(job.name(), "1")
					.build();
			Trigger trigger = TriggerBuilder.newTrigger()
					.withSchedule(CronScheduleBuilder.cronSchedule(job.cron()))
					.withIdentity(job.name(), "1")
					.build();

			try {
				sched.scheduleJob(detail, trigger);
				MiscHelper.logger(this.getClass()).info("Cron initialized: " + job.name());
			} catch (SchedulerException e) {
				MiscHelper.logger(this.getClass()).error("Failed to initialize cron (" + job.name() + "): " + e);
			}
		}

		sched.start();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {

	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {

	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		MiscHelper.logger(this.getClass()).debug("Programação executada em " + context.getFireTime() + ".\nPróxima execução em " + context.getNextFireTime());
	}

	public static void shutdown() {
		try {
			if (self != null) {
				self.sched.shutdown();
			}
		} catch (SchedulerException e) {
			MiscHelper.logger(ScheduledEvents.class).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}

