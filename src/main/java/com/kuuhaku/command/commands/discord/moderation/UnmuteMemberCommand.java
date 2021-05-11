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
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Command(
		name = "desmutar",
		aliases = {"unmute", "dessilenciar", "unsilence"},
		usage = "req_mention",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class UnmuteMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(I18n.getString("err_mention-required")).queue();
			return;
		} else if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
			channel.sendMessage("❌ | Você não possui permissão para dessilenciar membros.").queue();
			return;
		} else if (Helper.hasRoleHigherThan(message.getMentionedMembers().get(0), member)) {
			channel.sendMessage("❌ | Você não pode dessilenciar membros que possuem o mesmo cargo ou maior.").queue();
			return;
		} else if (MemberDAO.getMutedMemberById(message.getMentionedMembers().get(0).getId()) == null) {
			channel.sendMessage("❌ | Esse membro não está silenciado.").queue();
			return;
		}

		Member mb = message.getMentionedMembers().get(0);

		MutedMember m = MemberDAO.getMutedMemberById(mb.getId());
		List<Role> roles = m.getRoles().toList().stream().map(rol -> guild.getRoleById((String) rol)).filter(Objects::nonNull).collect(Collectors.toList());
		guild.modifyMemberRoles(mb, roles)
				.flatMap(s -> channel.sendMessage("✅ | Usuário dessilenciado com sucesso!"))
				.queue(s -> {
					Helper.logToChannel(author, false, null, mb.getAsMention() + " foi dessilenciado por " + author.getAsMention(), guild);
					MemberDAO.removeMutedMember(m);
				}, Helper::doNothing);
	}
}
