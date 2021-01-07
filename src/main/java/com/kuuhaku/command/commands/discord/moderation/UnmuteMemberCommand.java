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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnmuteMemberCommand extends Command {

	public UnmuteMemberCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public UnmuteMemberCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public UnmuteMemberCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public UnmuteMemberCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-member-to-ban")).queue();
			return;
		} else if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
			channel.sendMessage("❌ | Você não possui permissão para dessilenciar membros.").queue();
			return;
		} else if (!Helper.hasRoleHigherThan(member, message.getMentionedMembers().get(0))) {
			channel.sendMessage("❌ | Você não pode dessilenciar membros que possuem o mesmo cargo ou maior.").queue();
			return;
		} else if (MemberDAO.getMutedMemberById(message.getMentionedMembers().get(0).getId()) == null) {
			channel.sendMessage("❌ | Este membro não está silenciado.").queue();
			return;
		}

		Member mb = message.getMentionedMembers().get(0);

		try {
			MutedMember m = MemberDAO.getMutedMemberById(mb.getId());
			List<Role> roles = m.getRoles().toList().stream().map(rol -> guild.getRoleById((String) rol)).filter(Objects::nonNull).collect(Collectors.toList());
			guild.modifyMemberRoles(mb, roles).queue(null, Helper::doNothing);
			MemberDAO.removeMutedMember(m);

			Helper.logToChannel(author, false, null, mb.getAsMention() + " foi dessilenciado por " + author.getAsMention(), guild);
			channel.sendMessage("✅ | Usuário dessilenciado com sucesso!").queue();
		} catch (InsufficientPermissionException | HierarchyException e) {
			channel.sendMessage("❌ | Não possuo a permissão para dessilenciar membros.").queue();
		}
	}
}
