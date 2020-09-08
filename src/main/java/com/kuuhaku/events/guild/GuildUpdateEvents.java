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

package com.kuuhaku.events.guild;

import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.persistence.NoResultException;

public class GuildUpdateEvents extends ListenerAdapter {

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
        gc.setName(event.getNewName());
        GuildDAO.updateGuildSettings(gc);
    }

    @Override
    public void onGuildUpdateOwner(@Nonnull GuildUpdateOwnerEvent event) {
		assert event.getOldOwner() != null;
		assert event.getNewOwner() != null;

		if (TagDAO.getTagById(event.getOldOwner().getId()).isBeta()) {
			TagDAO.removeTagBeta(event.getOldOwner().getId());

			try {
				TagDAO.getTagById(event.getNewOwner().getId());
			} catch (NoResultException e) {
				TagDAO.addUserTagsToDB(event.getNewOwner().getId());
			} finally {
				TagDAO.giveTagBeta(event.getNewOwner().getId());
			}
		}
	}
}