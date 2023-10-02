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
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "perdoar",
		aliases = {"pardon"},
		usage = "req_mention-id-warn",
		category = Category.MODERATION
)
public class PardonMemberCommand implements Executable {

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
		} else if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			channel.sendMessage(I18n.getString("err_pardon-not-allowed")).queue();
			return;
		} else if (!member.canInteract(mb)) {
			channel.sendMessage(I18n.getString("err_cannot-pardon-higher-role")).queue();
			return;
		} else if (!guild.getSelfMember().canInteract(mb)) {
			channel.sendMessage(I18n.getString("err_cannot-pardon-higher-role-me")).queue();
			return;
		}

		com.kuuhaku.model.persistent.Member m = MemberDAO.getMember(mb.getId(), guild.getId());
		if (args.length < 2) {
			List<List<String>> chunks = Helper.chunkify(m.getWarns(), 10);
			if (chunks.isEmpty()) {
				channel.sendMessage("❌ | Não há nenhum alerta para esse usuário.").queue();
				return;
			}

			List<Page> pages = new ArrayList<>();
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":tickets: | Alertas de " + mb.getUser().getName());
			int i = 0;
			for (List<String> chunk : chunks) {
				eb.clearFields();

				for (String warn : chunk) {
					eb.addField("`ID: " + i + "`", warn, false);
					i++;
				}

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			int i = Integer.parseInt(args[1]);
			if (!Helper.between(i, 0, m.getWarns().size())) {
				channel.sendMessage("❌ | Alerta inexistente.").queue();
				return;
			}

			String reason = m.getWarns().remove(i);
			MemberDAO.saveMember(m);

			channel.sendMessage("✅ | Alerta perdoado com sucesso!").queue();
			Helper.logToChannel(author, false, null, mb.getAsMention() + " teve um alerta perdoado.\nAlerta: `" + reason + "`", guild);
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-index")).queue();
		}
	}
}
