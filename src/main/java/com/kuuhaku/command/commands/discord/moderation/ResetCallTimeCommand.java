/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.VoiceTimeDAO;
import com.kuuhaku.model.annotations.Command;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "zerarcall",
		aliases = {"resetcall", "zerartempo"},
		usage = "req_mention-opt",
		category = Category.MODERATION
)
public class ResetCallTimeCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().isEmpty()) {
			VoiceTimeDAO.resetVoiceTimes(guild.getId());

			channel.sendMessage("✅ | Tempos de call reiniciados com sucesso!").queue();
		} else {
			User u = message.getMentionedUsers().get(0);

			VoiceTimeDAO.removeVoiceTime(VoiceTimeDAO.getVoiceTime(u.getId(), guild.getId()));
			channel.sendMessage("✅ | Tempo de call de " + u.getAsMention() + " reiniciado com sucesso!").queue();
		}
	}
}
