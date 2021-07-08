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
import org.apache.commons.lang3.StringUtils;

@Command(
		name = "canaldestaques",
		aliases = {"canaldtk", "starboard"},
		usage = "req_channel-reset-amount",
		category = Category.MODERATION
)
public class StarboardChannelCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		if (message.getMentionedChannels().isEmpty() && args.length == 0) {
			TextChannel chn = gc.getStarboardChannel();
			if (chn == null)
				channel.sendMessage("Ainda não foi definido um canal de destaques.").queue();
			else
				channel.sendMessage("O canal de destaques atual do servidor é " + chn.getAsMention() + " (" + gc.getStarRequirement() + " estrelas).").queue();
			return;
		}

		try {
			if (Helper.equalsAny(args[0], "limpar", "reset")) {
				gc.setStarboardChannel(null);
				channel.sendMessage("✅ | Canal de destaques limpo com sucesso.").queue();
			} else if (StringUtils.isNumeric(args[0])) {
				int req = Integer.parseInt(args[0]);

				if (req < 2) {
					channel.sendMessage("❌ | O valor mínimo de estrelas necessárias para o canal de destaques é 2.").queue();
					return;
				}

				gc.setStarRequirement(req);
				channel.sendMessage("✅ | Quantidade de estrelas para a mensagem ser destacada definido como " + req + " estrelas.").queue();
			} else {
				gc.setStarboardChannel(message.getMentionedChannels().get(0).getId());
				channel.sendMessage("✅ | Canal de destaques definido com sucesso.").queue();
			}
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Você precisa mencionar um canal, digitar `limpar` ou informar um valor.").queue();
			return;
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O valor deve ser numérico.").queue();
			return;
		}

		GuildDAO.updateGuildSettings(gc);
	}
}
