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
		name = "banir",
		aliases = {"ban"},
		usage = "req_mentions-ids-reason",
		category = Category.MODERATION
)
@Requires({Permission.BAN_MEMBERS})
public class BanMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedMembers().isEmpty()) {
			channel.sendMessage(I18n.getString("err_mention-required")).queue();
			return;
		} else if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			channel.sendMessage(I18n.getString("err_ban-not-allowed")).queue();
			return;
		}

		for (Member mb : message.getMentionedMembers()) {
			if (!member.canInteract(mb)) {
				channel.sendMessage(I18n.getString("err_cannot-ban-high-role")).queue();
				return;
			} else if (!guild.getSelfMember().canInteract(mb)) {
				channel.sendMessage(I18n.getString("err_cannot-ban-high-role-me")).queue();
				return;
			} else if (ShiroInfo.getStaff().contains(mb.getId())) {
				channel.sendMessage(I18n.getString("err_cannot-ban-staff")).queue();
				return;
			}
		}

		String reason = argsAsText.replaceAll(Helper.MENTION, "").trim();
		if (message.getMentionedMembers().size() > 1) {
			if (reason.isBlank()) {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.ban(7));
				}

				RestAction.allOf(acts)
						.mapToResult()
						.flatMap(s -> channel.sendMessage("✅ | Membros banidos com sucesso!"))
						.queue(null, Helper::doNothing);
			} else {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.ban(7, reason));
				}

				RestAction.allOf(acts)
						.mapToResult()
						.flatMap(s -> channel.sendMessage("✅ | Membros banidos com sucesso!\nRazão: `" + reason + "`"))
						.queue(null, Helper::doNothing);
			}
		} else {
			if (reason.isBlank()) {
				message.getMentionedMembers().get(0).ban(7)
						.flatMap(s -> channel.sendMessage("✅ | Membro banido com sucesso!"))
						.queue(null, Helper::doNothing);
			} else {
				message.getMentionedMembers().get(0).ban(7, reason)
						.flatMap(s -> channel.sendMessage("✅ | Membro banido com sucesso!\nRazão: `" + reason + "`"))
						.queue(null, Helper::doNothing);
			}
		}
	}
}
