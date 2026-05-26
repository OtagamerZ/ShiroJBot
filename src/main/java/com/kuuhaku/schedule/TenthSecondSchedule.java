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
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.Calc;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

import java.util.List;
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

		int memberCount = guild.getMemberCount();
		GuildConfig config = DAO.find(GuildConfig.class, guild.getId());
		for (GuildVoiceState state : states) {
			AudioChannelUnion chn = state.getChannel();
			if (chn == null) continue;

			Account acc = DAO.find(Account.class, state.getId());
			if (acc != null) {
				int xp = config.getXpGained(acc) * 10;

				double mult = 0.2 + Math.min(Calc.prcnt(memberCount, 1000), 1) * 0.4;
				if (state.isStream()) {
					mult *= 1.4;
				}

				int members = (int) chn.getMembers().stream().filter(m -> !m.getUser().isBot()).count();
				mult *= Math.min(Calc.prcnt(members, 3), 1);

				acc.getProfile(guild).addXp((int) (xp * mult));
			}
		}
	}
}
