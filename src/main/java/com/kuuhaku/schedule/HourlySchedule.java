/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.schedule;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.PreInitialize;
import com.kuuhaku.interfaces.annotations.Schedule;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Reminder;
import com.kuuhaku.util.API;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.entities.User;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpHeaders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Schedule("0 * * * *")
public class HourlySchedule implements Runnable, PreInitialize {
	public static final Set<Integer> SCHED_REMINDERS = new HashSet<>();
	private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void run() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		List<Reminder> rems = DAO.queryAll(Reminder.class, "SELECT r FROM Reminder r WHERE NOT r.reminded AND r.due <= ?1 AND r.id NOT IN ?2",
				now.plusHours(1).truncatedTo(ChronoUnit.HOURS), SCHED_REMINDERS
		);
		for (Reminder r : rems) {
			scheduleReminder(r);
		}

		List<Account> accs = DAO.queryAll(Account.class, "SELECT a FROM Account a WHERE NOT a.voteAwarded AND a.lastVote IS NOT NULL");
		for (Account a : accs) {
			a.checkVote();
		}

		int guilds = (int) Main.getApp().getShiro().getGuildCache().size();
		API.call(
				new HttpPost("https://top.gg/api/bots/" + Main.getApp().getId() + "/stats"), null,
				JSONObject.of(
						Map.entry(HttpHeaders.AUTHORIZATION, Constants.TOPGG_TOKEN)
				),
				JSONObject.of(
						Map.entry("server_count", guilds)
				).toString()
		);
	}

	public static void scheduleReminder(Reminder r) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-3"));
		SCHED_REMINDERS.add(r.getId());
		exec.schedule(() -> {
			try {
				Account acc = r.getAccount();
				I18N locale = acc.getEstimateLocale();
				User u = acc.getUser();

				if (r.getChannel() != null && r.getChannel().canTalk()) {
					r.getChannel().sendMessage(locale.get("str/reminder", u.getAsMention(), r.getMessage())).queue();
				} else {
					u.openPrivateChannel()
							.flatMap(c -> c.sendMessage(locale.get("str/reminder", u.getAsMention(), r.getMessage())))
							.queue(null, Utils::doNothing);
				}
			} finally {
				SCHED_REMINDERS.remove(r.getId());
				r.setReminded(true);
				r.save();
			}
		}, now.until(r.getDue(), ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
	}
}
