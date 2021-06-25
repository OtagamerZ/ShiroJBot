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
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "spawncartas",
		aliases = {"habilitarkp", "pkp", "ekp", "allowcards"},
		usage = "req_channel",
		category = Category.MODERATION
)
public class AllowKawaiponCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (message.getMentionedChannels().size() < 1) {
			gc.toggleCardSpawn();
			gc.setKawaiponChannel(null);

			if (gc.isCardSpawn())
				channel.sendMessage("✅ | Agora aparecerão cartas Kawaipon neste servidor.").queue();
			else
				channel.sendMessage("✅ | Não aparecerão mais cartas Kawaipon.").queue();
		} else {
			gc.setCardSpawn(true);
			gc.setKawaiponChannel(message.getMentionedChannels().get(0).getId());
			channel.sendMessage("✅ | Agora aparecerão cartas Kawaipon no canal " + message.getMentionedChannels().get(0).getAsMention() + ".").queue();
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
