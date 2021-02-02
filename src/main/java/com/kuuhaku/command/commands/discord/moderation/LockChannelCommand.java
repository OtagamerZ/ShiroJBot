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
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "trancar",
		aliases = {"lock", "travar"},
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_PERMISSIONS})
public class LockChannelCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!Helper.hasPermission(member, Permission.MANAGE_PERMISSIONS, channel)) {
			channel.sendMessage("❌ | Você não possui permissão para alterar permissões de canais.").queue();
			return;
		} else if (!Helper.hasPermission(guild.getSelfMember(), Permission.MANAGE_PERMISSIONS, channel)) {
			channel.sendMessage("❌ | Eu não possuo permissão para alterar permissões de canais.").queue();
			return;
		}

		try {
			List<PermissionOverride> overrides = channel.getPermissionOverrides();
			List<PermissionOverrideAction> acts = new ArrayList<>();

			acts.add(channel.upsertPermissionOverride(guild.getSelfMember()).grant(Permission.MESSAGE_WRITE, Permission.MANAGE_PERMISSIONS));
			acts.add(channel.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.MESSAGE_WRITE));
			for (PermissionOverride override : overrides) {
				IPermissionHolder holder = override.getPermissionHolder();
				if (holder != null)
					acts.add(channel.upsertPermissionOverride(holder).deny(Permission.MESSAGE_WRITE));
			}

			RestAction.accumulate(acts, Collectors.toList()).complete();

			channel.sendMessage(":lock: | Canal trancado com sucesso!").queue();
		} catch (InsufficientPermissionException e) {
			channel.sendMessage("❌ | Não possuo a permissão para alterar permissões de canais.").queue();
		}
	}
}
