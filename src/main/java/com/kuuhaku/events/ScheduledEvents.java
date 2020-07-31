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

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.events.cron.*;
import com.kuuhaku.utils.Helper;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class ScheduledEvents implements JobListener {
	private static Scheduler sched;

	public ScheduledEvents() {
		Thread.currentThread().setName("backup");
		schedBackup();
		schedClear();
		schedUnblock();
		schedCheck();
		schedRefreshWinner();
		schedMarkWinner();

		if (Main.getInfo().getDblApi() != null) schedUpdateServerCount();
		schedUpdateKawaigotchi();
	}

	private void schedCheck() {
		try {
			if (PartnerCheckEvent.check == null) {
				PartnerCheckEvent.check = JobBuilder.newJob(PartnerCheckEvent.class).withIdentity("check", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("check", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(PartnerCheckEvent.check, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: check");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma check: " + e);
		}
	}

	private void schedClear() {
		try {
			if (ClearEvent.clear == null) {
				ClearEvent.clear = JobBuilder.newJob(ClearEvent.class).withIdentity("clear", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("clear", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(ClearEvent.clear, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: clear");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma clear: " + e);
		}
	}
	
	private void schedBackup() {
		try {
			if (BackupEvent.backup == null) {
				BackupEvent.backup = JobBuilder.newJob(BackupEvent.class).withIdentity("backup", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("backup", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/1 ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(BackupEvent.backup, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: backup");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma backup: " + e);
		}
	}

	private void schedUnblock() {
		try {
			if (UnblockEvent.unblock == null) {
				UnblockEvent.unblock = JobBuilder.newJob(UnblockEvent.class).withIdentity("unblock", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("unblock", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 * 1/1 * ? *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(UnblockEvent.unblock, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: unblock");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma unblock: " + e);
		}
	}

	private void schedRefreshWinner() {
		try {
			if (GetWinnerEvent.refreshWinner == null) {
				GetWinnerEvent.refreshWinner = JobBuilder.newJob(GetWinnerEvent.class).withIdentity("refreshWinner", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("refreshWinner", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/1 ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(GetWinnerEvent.refreshWinner, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: refreshWinner");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma refreshWinner: " + e);
		}
	}

	private void schedMarkWinner() {
		try {
			if (MarkWinnerEvent.markWinner == null) {
				MarkWinnerEvent.markWinner = JobBuilder.newJob(MarkWinnerEvent.class).withIdentity("markWinner", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("markWinner", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 1/1 ? *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(MarkWinnerEvent.markWinner, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: markWinner");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma markWinner: " + e);
		}
	}

	private void schedUpdateServerCount() {
		try {
			if (UpdateServerCountEvent.updateServerCount == null) {
				UpdateServerCountEvent.updateServerCount = JobBuilder.newJob(UpdateServerCountEvent.class).withIdentity("updateServerCount", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("updateServerCount", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(UpdateServerCountEvent.updateServerCount, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: updateServerCount");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma updateServerCount: " + e);
		}
	}

	private void schedUpdateKawaigotchi() {
		try {
			if (UpdateKawaigotchiEvent.updateKawaigotchi == null) {
				UpdateKawaigotchiEvent.updateKawaigotchi = JobBuilder.newJob(UpdateKawaigotchiEvent.class).withIdentity("updateKawaigotchi", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("updateKawaigotchi", "1").withSchedule(CronScheduleBuilder.cronSchedule("0/2 * * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(UpdateKawaigotchiEvent.updateKawaigotchi, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso: updateKawaigotchi");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma updateKawaigotchi: " + e);
		}
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
		Helper.logger(this.getClass()).info("Programação executada em " + context.getFireTime() + ".\nPróxima execução em " + context.getNextFireTime());
	}
}

