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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.VoiceTimeDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
			channel.sendMessage("Isso vai limpar o tempo de call acumulado de todos os membros neste servidor, tem certeza?").queue(
					s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								VoiceTimeDAO.resetVoiceTimes(guild.getId());
								channel.sendMessage("✅ | Tempos de call reiniciados com sucesso!").queue();
							}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES
							, u -> u.getId().equals(author.getId())
					), MiscHelper::doNothing
			);
		} else {
			User tgt = message.getMentionedUsers().get(0);

			channel.sendMessage("Isso vai limpar o tempo de call acumulado de " + tgt.getName() + ", tem certeza?").queue(
					s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								VoiceTimeDAO.removeVoiceTime(VoiceTimeDAO.getVoiceTime(tgt.getId(), guild.getId()));
								channel.sendMessage("✅ | Tempo de call de " + tgt.getAsMention() + " reiniciado com sucesso!").queue();
							}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES
							, u -> u.getId().equals(author.getId())
					), MiscHelper::doNothing
			);
		}
	}
}
