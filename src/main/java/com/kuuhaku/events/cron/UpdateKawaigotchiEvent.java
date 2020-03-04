/*
 * This file is part of Shiro J Bot.
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
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.List;

public class UpdateKawaigotchiEvent implements Job {
	public static JobDetail updateKawaigotchi;

	@Override
	public void execute(JobExecutionContext context) {
		List<Kawaigotchi> kgs = KGotchiDAO.getAllKawaigotchi();

		kgs.forEach(k -> {
			try {
				k.update(Main.getInfo().getMemberByID(k.getUserId()));
			} catch (NullPointerException ignore) {
			}
		});
	}
}
