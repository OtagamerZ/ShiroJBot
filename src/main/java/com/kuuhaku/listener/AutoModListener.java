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
import com.kuuhaku.model.enums.AutoModType;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.model.persistent.user.Profile;
import net.dv8tion.jda.api.events.automod.AutoModExecutionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AutoModListener extends ListenerAdapter {
	@Override
	public void onAutoModExecution(@NotNull AutoModExecutionEvent event) {
		GuildConfig config = DAO.find(GuildConfig.class, event.getGuild().getId());
		AutoModType type = config.getSettings().getAutoModEntries().entrySet().parallelStream()
				.filter(e -> e.getValue().equals(event.getRuleId()))
				.map(Map.Entry::getKey)
				.findFirst().orElse(null);

		if (type == AutoModType.SPAM) {
			Profile p = DAO.find(Profile.class, new ProfileId(event.getUserId(), config.getGid()));
			p.warn(Main.getApp().getMainShard().getSelfUser(), "SPAM");
			p.save();
		}
	}
}
