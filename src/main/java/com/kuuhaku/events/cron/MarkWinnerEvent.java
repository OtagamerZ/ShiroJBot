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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class MarkWinnerEvent implements Job {
	public static JobDetail markWinner;

	@Override
	public void execute(JobExecutionContext context) {
		ExceedDAO.markWinner(ExceedDAO.findWinner());
		Helper.logger(this.getClass()).info("Vencedor mensal: " + ExceedDAO.getWinner());

		String ex = ExceedDAO.getWinner();
		ExceedDAO.getExceedMembers(ExceedEnums.getByName(ex)).forEach(em ->
				Main.getInfo().getUserByID(em.getId()).openPrivateChannel().queue(c -> {
					try {
						c.sendMessage("O seu exceed foi campeão neste mês, parabéns!\n" +
								"Todos da " + ex + " ganharão experiência em dobro durante 1 semana.").queue();
					} catch (Exception ignore) {
					}
				}));

		ExceedDAO.unblock();
	}
}
