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
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "removeremote",
		aliases = {"removeremoji", "remote", "remoji"},
		usage = "req_emotes",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_EMOTES})
public class RemoveEmoteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getEmotes().size() == 0) {
			channel.sendMessage("❌ | Você precisa informar ao menos 1 emote para remover.").queue();
			return;
		}

		List<Emote> filteredList = message.getEmotes().stream().filter(e -> guild.getEmoteById(e.getId()) != null).collect(Collectors.toList());
		List<AuditableRestAction<Void>> acts = new ArrayList<>();
		int removed = 0;
		for (Emote emote : filteredList) {
			acts.add(emote.delete());
			removed++;
		}

		int finalRemoved = removed;
		RestAction.allOf(acts)
				.mapToResult()
				.flatMap(s -> channel.sendMessage("✅ | " + finalRemoved + " emotes removidos com sucesso!"))
				.queue(null, Helper::doNothing);
	}
}
