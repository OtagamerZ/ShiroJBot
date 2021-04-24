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
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.util.ArrayList;
import java.util.List;

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
		if (message.getMentionedMembers().isEmpty()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_mention-required")).queue();
			return;
		} else if (!member.hasPermission(Permission.KICK_MEMBERS)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_kick-not-allowed")).queue();
			return;
		}

		for (Member mb : message.getMentionedMembers()) {
			if (Helper.hasRoleHigherThan(mb, member)) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-kick-higher-role")).queue();
				return;
			} else if (ShiroInfo.getStaff().contains(mb.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-kick-staff")).queue();
				return;
			} else if (Helper.hasRoleHigherThan(mb, guild.getSelfMember())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-kick-high-role-me")).queue();
				return;
			}
		}

		String reason = argsAsText.replaceAll(Helper.MENTION, "").trim();
		if (message.getMentionedMembers().size() > 1) {
			if (reason.isBlank()) {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.kick());
				}

				RestAction.allOf(acts)
						.mapToResult()
						.flatMap(s -> channel.sendMessage("✅ | Membros expulsos com sucesso!"))
						.queue(null, Helper::doNothing);
			} else {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.kick(reason));
				}

				RestAction.allOf(acts)
						.mapToResult()
						.flatMap(s -> channel.sendMessage("✅ | Membros expulsos com sucesso!\nRazão: `" + reason + "`"))
						.queue(null, Helper::doNothing);
			}
		} else {
			if (reason.isBlank()) {
				message.getMentionedMembers().get(0).kick()
						.flatMap(s -> channel.sendMessage("✅ | Membro expulso com sucesso!"))
						.queue(null, Helper::doNothing);
			} else {
				message.getMentionedMembers().get(0).kick(reason)
						.flatMap(s -> channel.sendMessage("✅ | Membro expulso com sucesso!\nRazão: `" + reason + "`"))
						.queue(null, Helper::doNothing);
			}
		}
    }
}
