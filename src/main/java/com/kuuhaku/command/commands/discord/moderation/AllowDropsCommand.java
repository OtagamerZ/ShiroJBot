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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "spawndrops",
		aliases = {"habilitardp", "pdp", "edp", "allowdrops"},
		usage = "req_channel",
		category = Category.MODERATION
)
public class AllowDropsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (TagDAO.getTagById(guild.getOwnerId()).isToxic()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_toxic-owner")).queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (message.getMentionedChannels().size() < 1) {
			if (gc.isDropEnabled()) {
				gc.toggleDrop();
				gc.setCanalDrop(null);
				channel.sendMessage("Não aparecerão mais drops.").queue();
			} else {
				gc.toggleDrop();
				gc.setCanalDrop(null);
				channel.sendMessage("Agora aparecerão drops neste servidor.").queue();
			}
		} else {
			gc.setDropEnabled(true);
			gc.setCanalDrop(message.getMentionedChannels().get(0).getId());
			channel.sendMessage("Agora aparecerão drops no canal " + message.getMentionedChannels().get(0).getAsMention() + ".").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
