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

package com.kuuhaku.command.commands.discord.support;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.StaffDAO;
import com.kuuhaku.controller.postgresql.TicketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Ticket;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "convite",
		aliases = {"invite"},
		usage = "req_id",
		category = Category.SUPPORT
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class InviteCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(I18n.getString("err_no-ticket-id")).queue();
			return;
		}

		try {
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

			String role = StaffDAO.getUser(author.getId()).toString();
			Main.getInfo().getUserByID(t.getUid()).openPrivateChannel()
					.flatMap(s -> s.sendMessage("**ATUALIZAÇÃO DE TICKET:** Seu ticket número " + t.getNumber() + " será atendido por " + author.getAsTag() + " **(" + role + ")**\nPor favor seja detalhado e lembre-se que ajudaremos apenas sobre assuntos relacionados à Shiro.\n\n**Atenção:** nossa equipe jamais pedirá informações confidenciais como senhas ou emais, nunca forneça-os!"))
					.queue(null, Helper::doNothing);

			channel.sendMessage("Aqui está!\nhttps://discord.gg/" + t.getInvite()).queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(I18n.getString("err_invalid-ticket-id")).queue();
		}
	}
}