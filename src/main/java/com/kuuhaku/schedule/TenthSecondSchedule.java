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

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.PreInitialize;
import com.kuuhaku.interfaces.annotations.Schedule;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Profile;
import com.ygimenez.json.JSONArray;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Widget;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Schedule
public class TenthSecondSchedule implements Runnable, PreInitialize {
	private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void run() {
		exec.scheduleAtFixedRate(this::task, 0, 10, TimeUnit.SECONDS);
	}

	public void task() {
		Main.getApp().getShiro().getGuildCache().forEach(this::computeVoiceXp);
	}

	public void computeVoiceXp(Guild guild) {
		List<GuildVoiceState> states = guild.getVoiceStates().parallelStream()
				.filter(v -> v.inAudioChannel() && !v.isDeafened() && !v.isMuted())
				.toList();

		if (states.isEmpty()) return;

		GuildConfig config = DAO.find(GuildConfig.class, guild.getId());
		for (GuildVoiceState state : states) {
			Account acc = DAO.find(Account.class, state.getId());
			if (acc != null) {
				double mult = state.isStream() ? 0.8 : 0.5;
				int xp = config.getXpGained(acc);
				acc.getProfile(guild).addXp((int) (xp * mult));
			}
		}
	}
}
