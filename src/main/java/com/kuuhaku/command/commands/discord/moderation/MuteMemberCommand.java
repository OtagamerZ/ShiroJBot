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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Command(
		name = "silenciar",
		aliases = {"mute", "silence"},
		usage = "req_member-id-time-reason",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS})
public class MuteMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Member mb = null;
		if (args.length > 0 && StringUtils.isNumeric(args[0]))
			mb = guild.getMemberById(args[0]);
		else if (!message.getMentionedMembers().isEmpty())
			mb = message.getMentionedMembers().get(0);

		if (mb == null) {
			channel.sendMessage(I18n.getString("err_user-or-id-required")).queue();
			return;
		} else if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
			channel.sendMessage(I18n.getString("err_mute-not-allowed")).queue();
			return;
		} else if (!member.canInteract(mb)) {
			channel.sendMessage(I18n.getString("err_cannot-mute-higher-role")).queue();
			return;
		} else if (!guild.getSelfMember().canInteract(mb)) {
			channel.sendMessage(I18n.getString("err_cannot-mute-higher-role-me")).queue();
			return;
		} else if (ShiroInfo.getStaff().contains(mb.getId())) {
			channel.sendMessage(I18n.getString("err_cannot-mute-staff")).queue();
			return;
		} else if (MemberDAO.getMutedMemberById(mb.getId()) != null && MemberDAO.getMutedMemberById(mb.getId()).isMuted()) {
			channel.sendMessage("❌ | Esse membro já está silenciado.").queue();
			return;
		}

		List<String> params = Arrays.stream(argsAsText.split("([0-9]+[dhms])+"))
				.filter(s -> !s.isBlank())
				.map(String::trim)
				.toList();
		if (params.size() < 2) {
			channel.sendMessage("❌ | Você precisa informar um tempo.").queue();
			return;
		}

		MutedMember m = CollectionHelper.getOr(MemberDAO.getMutedMemberById(mb.getId()), new MutedMember(mb.getId(), guild.getId()));
		long time = StringHelper.stringToDurationMillis(argsAsText);
		if (time < 60000) {
			channel.sendMessage("❌ | O tempo deve ser maior que 1 minuto.").queue();
			return;
		}

		Member finalMb = mb;
		argsAsText = String.join(" ", params);
		String reason = argsAsText.replaceFirst("<@!?" + mb.getId() + ">|" + mb.getId(), "").trim();
		if (reason.isBlank()) {
			channel.sendMessage("❌ | Você precisa informar uma razão.").queue();
			return;
		}

		m.setReason(argsAsText);
		m.mute(time);

		List<PermissionOverrideAction> act = new ArrayList<>();
		for (GuildChannel gc : guild.getChannels()) {
			try {
				switch (gc.getType()) {
					case TEXT -> {
						TextChannel chn = (TextChannel) gc;

						if (chn.canTalk(mb))
							act.add(chn.putPermissionOverride(mb).deny(Constants.ALL_MUTE_PERMISSIONS));
					}
					case VOICE -> {
						VoiceChannel chn = (VoiceChannel) gc;

						act.add(chn.putPermissionOverride(mb).deny(Permission.VOICE_SPEAK));
					}
				}
			} catch (InsufficientPermissionException ignore) {
			}
		}

		if (act.isEmpty()) {
			channel.sendMessage("❌ | Não possuo permissão suficiente para silenciar esse usuário ou ele já não pode ver nenhum canal.").queue();
			return;
		}

		RestAction.allOf(act)
				.flatMap(s -> channel.sendMessage("✅ | Usuário silenciado por " + StringHelper.toStringDuration(time) + " com sucesso!\nRazão: `" + reason + "`"))
				.queue(s -> {
					MiscHelper.logToChannel(author, false, null, finalMb.getAsMention() + " foi silenciado por " + StringHelper.toStringDuration(time) + ".\nRazão: `" + reason + "`", guild);
					MemberDAO.saveMutedMember(m);
				}, MiscHelper::doNothing);
	}
}
