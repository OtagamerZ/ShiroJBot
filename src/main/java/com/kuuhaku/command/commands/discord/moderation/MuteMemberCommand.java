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
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONArray;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "silenciar",
		aliases = {"mute", "silence"},
		usage = "req_member-time-reason",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES})
public class MuteMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (message.getMentionedMembers().isEmpty()) {
			channel.sendMessage(I18n.getString("err_mention-required")).queue();
			return;
		} else if (args.length < 3) {
			channel.sendMessage("❌ | Você precisa informar um tempo (em minutos) e um motivo.").queue();
			return;
		} else if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
			channel.sendMessage(I18n.getString("err_mute-not-allowed")).queue();
			return;
		}

		Member mb = message.getMentionedMembers().get(0);

		if (!member.canInteract(mb)) {
			channel.sendMessage(I18n.getString("err_cannot-mute-higher-role")).queue();
			return;
		} else if (!guild.getSelfMember().canInteract(mb)) {
			channel.sendMessage(I18n.getString("err_cannot-mute-higher-role-me")).queue();
			return;
		} else if (ShiroInfo.getStaff().contains(mb.getId())) {
			channel.sendMessage(I18n.getString("err_cannot-mute-staff")).queue();
			return;
		} else if (gc.getMuteRole() == null) {
			channel.sendMessage("❌ | Nenhum cargo de mute configurado neste servidor.").queue();
			return;
		} else if (MemberDAO.getMutedMemberById(mb.getId()) != null && MemberDAO.getMutedMemberById(mb.getId()).isMuted()) {
			channel.sendMessage("❌ | Esse membro já está silenciado.").queue();
			return;
		}

		String reason = argsAsText.replaceAll(Helper.MENTION, "").trim();
		try {
			JSONArray roles = new JSONArray(mb.getRoles().stream()
					.filter(rl -> !rl.isManaged())
					.map(Role::getId)
					.collect(Collectors.toList()));

			MutedMember m = Helper.getOr(MemberDAO.getMutedMemberById(mb.getId()), new MutedMember(mb.getId(), guild.getId(), roles));
			int time = Integer.parseInt(args[1]);

			m.setReason(reason);
			m.setRoles(new JSONArray(mb.getRoles().stream().map(Role::getId).toArray(String[]::new)));
			m.mute(time);

			List<Role> rls = mb.getRoles().stream().filter(Role::isManaged).collect(Collectors.toList());
			rls.add(gc.getMuteRole());

			guild.modifyMemberRoles(mb, rls)
					.flatMap(s -> channel.sendMessage("✅ | Usuário silenciado por " + time + " minutos com sucesso!\nRazão: `" + reason + "`"))
					.queue(s -> {
						Helper.logToChannel(author, false, null, mb.getAsMention() + " foi silenciado.\nRazão: `" + reason + "`", guild);
						MemberDAO.saveMutedMember(m);
					}, Helper::doNothing);
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-amount")).queue();
		} catch (HierarchyException e) {
			channel.sendMessage("❌ | O cargo de mute está acima de mim.").queue();
		}
	}
}
