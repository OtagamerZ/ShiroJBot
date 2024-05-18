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

package com.kuuhaku.listener;

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PrivateChannelListener extends ListenerAdapter {
	@Override
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		List<String> devs = DAO.queryAllNative(String.class, "SELECT uid FROM account WHERE bool(role & 8)");
		for (String dev : devs) {
			Account acc = DAO.find(Account.class, dev);
			if (acc != null) {
				acc.getUser().openPrivateChannel()
						.flatMap(c -> c.sendMessage("Joined server: " + event.getGuild().getName() + " (" + event.getGuild().getMemberCount() + " members)"))
						.queue(null, Utils::doNothing);
			}
		}
	}

	@Override
	public void onGuildLeave(@NotNull GuildLeaveEvent event) {
		List<String> devs = DAO.queryAllNative(String.class, "SELECT uid FROM account WHERE bool(role & 8)");
		for (String dev : devs) {
			Account acc = DAO.find(Account.class, dev);
			if (acc != null) {
				acc.getUser().openPrivateChannel()
						.flatMap(c -> c.sendMessage("Left server: " + event.getGuild().getName() + " (" + event.getGuild().getMemberCount() + " members)"))
						.queue(null, Utils::doNothing);
			}
		}
	}
}
