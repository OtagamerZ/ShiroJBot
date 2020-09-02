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

import com.kuuhaku.events.cron.*;
import com.kuuhaku.utils.Helper;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class ScheduledEvents implements JobListener {
	private static Scheduler sched;

	public ScheduledEvents() {
		Thread.currentThread().setName("crontab");
		schedHourly();
		schedMinute();
		schedTenthMinute();
		schedMonthly();
		schedOddSecond();
	}

	private void schedTenthMinute() {
		try {
			if (TenthMinuteEvent.check == null) {
				TenthMinuteEvent.check = JobBuilder.newJob(TenthMinuteEvent.class).withIdentity("tenth", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("tenth", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(TenthMinuteEvent.check, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso a cada 10 minutos");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma a cada 10 minutos: " + e);
		}
	}

	private void schedHourly() {
		try {
			if (HourlyEvent.backup == null) {
				HourlyEvent.backup = JobBuilder.newJob(HourlyEvent.class).withIdentity("hourly", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("hourly", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(HourlyEvent.backup, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso a cada hora");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma a cada hora: " + e);
		}
	}

	private void schedMinute() {
		try {
			if (MinuteEvent.unblock == null) {
				MinuteEvent.unblock = JobBuilder.newJob(MinuteEvent.class).withIdentity("minute", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("minute", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ? *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(MinuteEvent.unblock, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso a cada minuto");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma a cada minuto: " + e);
		}
	}

	private void schedMonthly() {
		try {
			if (MonthlyEvent.markWinner == null) {
				MonthlyEvent.markWinner = JobBuilder.newJob(MonthlyEvent.class).withIdentity("monthly", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("monthly", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 * ? *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(MonthlyEvent.markWinner, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso a cada mês");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma a cada mês: " + e);
		}
	}

	private void schedOddSecond() {
		try {
			if (OddSecondEvent.updateKawaigotchi == null) {
				OddSecondEvent.updateKawaigotchi = JobBuilder.newJob(OddSecondEvent.class).withIdentity("oddsecond", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("oddsecond", "1").withSchedule(CronScheduleBuilder.cronSchedule("0/2 * * ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(OddSecondEvent.updateKawaigotchi, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				Helper.logger(this.getClass()).info("Cronograma inicializado com sucesso a cada segundo par");
			}
		} catch (SchedulerException e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar cronograma a cada segundo par: " + e);
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

