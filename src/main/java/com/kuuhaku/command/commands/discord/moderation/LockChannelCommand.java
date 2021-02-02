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
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.ArrayList;
import java.util.List;

@Command(
		name = "trancar",
		aliases = {"lock", "travar"},
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_PERMISSIONS})
public class LockChannelCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!Helper.hasPermission(member, Permission.MANAGE_CHANNEL, channel)) {
			channel.sendMessage("❌ | Você não possui permissão para gerenciar canais.").queue();
			return;
		}

		List<PermissionOverride> overrides = channel.getPermissionOverrides();
		List<PermissionOverrideAction> acts = new ArrayList<>();

		acts.add(channel.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.MESSAGE_WRITE));
		for (PermissionOverride override : overrides) {
			IPermissionHolder holder = override.getPermissionHolder();
			if (holder != null)
				acts.add(channel.upsertPermissionOverride(holder).deny(Permission.MESSAGE_WRITE));
		}
		if (guild.getBotRole() != null)
			acts.add(channel.upsertPermissionOverride(guild.getBotRole()).grant(Permission.MESSAGE_WRITE, Permission.MANAGE_PERMISSIONS));

		RestAction.allOf(acts)
				.flatMap(s -> channel.sendMessage(":lock: | Canal trancado com sucesso!"))
				.queue(null, f -> {
					f.printStackTrace();
					if (channel.canTalk())
						channel.sendMessage("❌ | Não possuo a permissão para alterar permissões de canais.").queue();
				});
	}
}
