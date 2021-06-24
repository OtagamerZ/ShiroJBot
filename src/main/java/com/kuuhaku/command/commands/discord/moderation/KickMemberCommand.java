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

import com.github.ygimenez.method.Pages;
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
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
		Set<Member> m = new HashSet<>();
		m.addAll(message.getMentionedMembers());
		m.addAll(Arrays.stream(args)
				.filter(StringUtils::isNumeric)
				.map(guild::getMemberById)
				.filter(Objects::nonNull)
				.collect(Collectors.toList()));

		if (m.isEmpty()) {
			channel.sendMessage(I18n.getString("err_user-or-id-required")).queue();
			return;
		} else if (!member.hasPermission(Permission.KICK_MEMBERS)) {
			channel.sendMessage(I18n.getString("err_kick-not-allowed")).queue();
			return;
		}

		for (Member mb : message.getMentionedMembers()) {
			if (!member.canInteract(mb)) {
				channel.sendMessage(I18n.getString("err_cannot-kick-higher-role")).queue();
				return;
			} else if (!guild.getSelfMember().canInteract(mb)) {
				channel.sendMessage(I18n.getString("err_cannot-kick-higher-role-me")).queue();
				return;
			} else if (ShiroInfo.getStaff().contains(mb.getId())) {
				channel.sendMessage(I18n.getString("err_cannot-kick-staff")).queue();
				return;
			}

			args = Arrays.stream(args)
					.filter(a -> !Helper.regex(a, "<@!?" + mb.getId() + ">|" + mb.getId()).find())
					.toArray(String[]::new);
		}

		String reason = String.join(" ", args);
		if (m.size() > 1) {
			if (reason.isBlank()) {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.kick());
				}

				channel.sendMessage("Você está prestes a expulsar " + m.stream().map(Member::getEffectiveName).collect(Collectors.collectingAndThen(Collectors.toList(), Helper.properlyJoin())) + ", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									RestAction.allOf(acts)
											.mapToResult()
											.flatMap(r -> channel.sendMessage("✅ | Membros banidos com sucesso!"))
											.flatMap(r -> s.delete())
											.queue(null, Helper::doNothing);
								}), true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			} else {
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (Member mb : message.getMentionedMembers()) {
					acts.add(mb.kick(reason));
				}

				channel.sendMessage("Você está prestes a expulsar " + m.stream().map(Member::getEffectiveName).collect(Collectors.collectingAndThen(Collectors.toList(), Helper.properlyJoin())) + " pela razão \"" + reason + "\", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									RestAction.allOf(acts)
											.mapToResult()
											.flatMap(r -> channel.sendMessage("✅ | Membros banidos com sucesso!\nRazão: `" + reason + "`"))
											.flatMap(r -> s.delete())
											.queue(null, Helper::doNothing);
								}), true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			}
		} else {
			Member mm = m.stream().findFirst().orElseThrow();

			if (reason.isBlank()) {
				channel.sendMessage("Você está prestes a expulsar " + mm.getEffectiveName() + ", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									mm.kick()
											.mapToResult()
											.flatMap(r -> channel.sendMessage("✅ | Membro expulso com sucesso!"))
											.flatMap(r -> s.delete())
											.queue(null, Helper::doNothing);
								}), true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			} else {
				channel.sendMessage("Você está prestes a expulsar " + mm.getEffectiveName() + ", deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									mm.kick(reason)
											.mapToResult()
											.flatMap(r -> channel.sendMessage("✅ | Membro expulso com sucesso!\nRazão: `" + reason + "`"))
											.flatMap(r -> s.delete())
											.queue(null, Helper::doNothing);
								}), true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			}
		}
    }
}
