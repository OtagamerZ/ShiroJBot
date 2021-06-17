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

package com.kuuhaku.command.commands.discord.support;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TicketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Ticket;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Command(
		name = "convite",
		aliases = {"invite"},
		usage = "req_id",
		category = Category.SUPPORT
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class InviteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_no-ticket-id")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(I18n.getString("err_invalid-ticket-id")).queue();
			return;
		}

		Ticket t = TicketDAO.getTicket(Integer.parseInt(args[0]));

		if (t == null) {
			channel.sendMessage(I18n.getString("err_invalid-ticket")).queue();
			return;
		} else if (t.isSolved()) {
			channel.sendMessage(I18n.getString("err_ticket-already-solved")).queue();
			return;
		}

		if (t.getInvite().isBlank()) {
			channel.sendMessage(I18n.getString("err_assist-not-requested")).queue();
			return;
		}

		String role = "";
		if (ShiroInfo.getSupports().containsKey(author.getId())) {
			role = "SUPORTE";
		} else if (ShiroInfo.getDevelopers().contains(author.getId())) {
			role = "DESENVOLVEDOR";
		}

		String finalRole = role;
		Main.getInfo().getUserByID(t.getUid()).openPrivateChannel()
				.flatMap(s -> s.sendMessage("**ATUALIZAÇÃO DE TICKET:** Seu ticket número " + t.getNumber() + " será atendido por " + author.getAsTag() + " (" + finalRole + ")"))
				.queue(null, Helper::doNothing);

		Guild g = Main.getInfo().getGuildByID(t.getSid());
		List<Invite> invs = g.retrieveInvites().complete();
		Invite iv = invs.stream()
				.filter(i -> i.getCode().equals(t.getInvite()))
				.findFirst()
				.orElse(null);

		if (iv == null) {
			channel.sendMessage("❌ | O convite desse ticket não é mais válido.").queue();
			return;
		}

		channel.sendMessage("Aqui está!\n" + iv.getUrl()).queue();
	}
}
