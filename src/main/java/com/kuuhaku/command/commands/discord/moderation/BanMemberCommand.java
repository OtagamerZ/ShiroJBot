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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.StaffDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.StaffType;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "banir",
		aliases = {"ban"},
		usage = "req_mentions-ids-reason",
		category = Category.MODERATION
)
@Requires({Permission.BAN_MEMBERS})
public class BanMemberCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Set<Member> m = new HashSet<>();
		m.addAll(message.getMentionedMembers());
		m.addAll(Arrays.stream(args)
				.filter(StringUtils::isNumeric)
				.map(guild::getMemberById)
				.filter(Objects::nonNull)
				.toList());

		if (m.isEmpty()) {
			channel.sendMessage(I18n.getString("err_user-or-id-required")).queue();
			return;
		} else if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			channel.sendMessage(I18n.getString("err_ban-not-allowed")).queue();
			return;
		}

		for (Member mb : m) {
			if (!member.canInteract(mb)) {
				channel.sendMessage(I18n.getString("err_cannot-ban-high-role")).queue();
				return;
			} else if (!guild.getSelfMember().canInteract(mb)) {
				channel.sendMessage(I18n.getString("err_cannot-ban-high-role-me")).queue();
				return;
			} else if (StaffDAO.getUser(mb.getId()).getType() != StaffType.NONE) {
				channel.sendMessage(I18n.getString("err_cannot-ban-staff")).queue();
				return;
			}

			argsAsText = argsAsText.replaceFirst("<@!?" + mb.getId() + ">|" + mb.getId(), "").trim();
		}

		String finalArgsAsText = argsAsText;
		if (m.size() > 1) {
			if (argsAsText.isBlank()) {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.ban(7));
				}

				channel.sendMessage("Você está prestes a banir " + Helper.parseAndJoin(m, IMentionable::getAsMention) + ", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper ->
										RestAction.allOf(acts)
												.flatMap(r -> channel.sendMessage("✅ | Membros banidos com sucesso!"))
												.flatMap(r -> s.delete())
												.queue(null, t -> channel.sendMessage("❌ | Erro ao banir.").queue())
								), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			} else {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.ban(7, argsAsText));
				}

				channel.sendMessage("Você está prestes a banir " + Helper.parseAndJoin(m, IMentionable::getAsMention) + " pela razão \"" + argsAsText + "\", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper ->
										RestAction.allOf(acts)
												.flatMap(r -> channel.sendMessage("✅ | Membros banidos com sucesso!\nRazão: `" + finalArgsAsText + "`"))
												.flatMap(r -> s.delete())
												.queue(null, t -> channel.sendMessage("❌ | Erro ao banir.").queue())
								), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			}
		} else {
			Member mm = m.stream().findFirst().orElseThrow();

			if (argsAsText.isBlank()) {
				channel.sendMessage("Você está prestes a banir " + mm.getUser().getName() + ", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper ->
										mm.ban(7)
												.flatMap(r -> channel.sendMessage("✅ | Membro banido com sucesso!"))
												.flatMap(r -> s.delete())
												.queue(null, t -> channel.sendMessage("❌ | Erro ao banir.").queue())
								), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			} else {
				channel.sendMessage("Você está prestes a banir " + mm.getUser().getName() + ", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper ->
										mm.ban(7, finalArgsAsText)
												.flatMap(r -> channel.sendMessage("✅ | Membro banido com sucesso!\nRazão: `" + finalArgsAsText + "`"))
												.flatMap(r -> s.delete())
												.queue(null, t -> channel.sendMessage("❌ | Erro ao banir.").queue())
								), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			}
		}
	}
}
