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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

@Command(
		name = "expulsar",
		aliases = {"kick"},
		usage = "req_mention-id-reason",
		category = Category.MODERATION
)
@Requires({Permission.KICK_MEMBERS})
public class KickMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-member-to-ban")).queue();
			return;
		} else if (message.getMentionedUsers().size() > 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_too-many-mentions")).queue();
			return;
		} else if (!member.hasPermission(Permission.KICK_MEMBERS)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-kick")).queue();
			return;
		} else if (!Helper.hasRoleHigherThan(member, message.getMentionedMembers().get(0)) || !Helper.hasRoleHigherThan(guild.getSelfMember(), message.getMentionedMembers().get(0))) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-kick-higher-role")).queue();
			return;
		} else if (ShiroInfo.getDevelopers().contains(message.getMentionedUsers().get(0).getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-kick-developers")).queue();
			return;
		}

        try {
            if (args.length < 2) {
				guild.kick(message.getMentionedMembers().get(0)).queue();
				channel.sendMessage("✅ | Membro expulso com sucesso!").queue();
			} else {
				guild.kick(message.getMentionedMembers().get(0), String.join(" ", args).replace(args[0], "").trim()).queue();
				channel.sendMessage("✅ | Membro expulso com sucesso!\nMotivo: `" + String.join(" ", args).replace(args[0], "").trim() + "`").queue();
			}
        } catch (InsufficientPermissionException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_kick-permission")).queue();
		}
    }
}
