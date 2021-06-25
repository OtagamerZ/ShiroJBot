/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "canalavisos",
		aliases = {"canalav", "alertchannel"},
		usage = "req_channel-reset",
		category = Category.MODERATION
)
public class AlertChannelCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (message.getMentionedChannels().isEmpty() && args.length == 0) {
			TextChannel chn = gc.getAlertChannel();
			if (chn == null)
				channel.sendMessage("Ainda não foi definido um canal de avisos.").queue();
			else
				channel.sendMessage("O canal de avisos atual do servidor é " + chn.getAsMention() + ".").queue();
			return;
		}

		try {
			if (Helper.equalsAny(args[0], "limpar", "reset")) {
				gc.setAlertChannel(null);
				channel.sendMessage("✅ | Canal de avisos limpo com sucesso.").queue();
			} else {
				gc.setAlertChannel(message.getMentionedChannels().get(0).getId());
				channel.sendMessage("✅ | Canal de avisos definido com sucesso.").queue();
			}
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Você precisa mencionar um canal ou digitar `limpar`.").queue();
			return;
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
