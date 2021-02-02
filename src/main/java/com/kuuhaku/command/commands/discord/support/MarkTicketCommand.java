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
import com.kuuhaku.controller.postgresql.RatingDAO;
import com.kuuhaku.controller.postgresql.TicketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.SupportRating;
import com.kuuhaku.model.persistent.Ticket;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Command(
		name = "resolver",
		aliases = {"mark", "solved", "solucionado", "fechar", "close"},
		usage = "req_id",
		category = Category.SUPPORT
)
public class MarkTicketCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-ticket-id")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-ticket-id")).queue();
			return;
		}

		SupportRating sr = RatingDAO.getRating(author.getId());
		Ticket t = TicketDAO.getTicket(Integer.parseInt(args[0]));

		if (t == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-ticket")).queue();
			return;
		} else if (t.isSolved()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_ticket-already-solved")).queue();
			return;
		}

		if (ShiroInfo.getStaff().contains(t.getRequestedBy())) {
			channel.sendMessage(":warning: | Ticket fechado mas sem efeito por ter sido aberto por um membro da equipe.").queue();
		} else {
			sr.addTicket();
			RatingDAO.saveRating(sr);

			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_successfully-solved-ticket")).queue();
		}

		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Resolução do ticket Nº " + args[0]);
		eb.setDescription("Assunto:```" + t.getSubject() + "```");
		if (Helper.getOr(t.getRequestedBy(), null) != null)
			eb.addField("Aberto por:", Main.getInfo().getUserByID(t.getRequestedBy()).getAsTag(), true);
		eb.addField("Resolvido por:", author.getAsTag(), true);
		eb.addField("Fechado em:", Helper.dateformat.format(LocalDateTime.now().atZone(ZoneId.of("GMT-3"))), true);
		eb.setColor(Color.green);

		for (String dev : ShiroInfo.getStaff()) {
			Message msg = Main.getInfo().getUserByID(dev).openPrivateChannel()
					.flatMap(m -> m.sendMessage(eb.build()))
					.complete();
			msg.getChannel().retrieveMessageById(String.valueOf(t.getMsgIds().get(dev)))
					.flatMap(Message::delete)
					.queue(null, Helper::doNothing);
			t.solved();
		}

		Main.getInfo().getUserByID(t.getRequestedBy()).openPrivateChannel()
				.flatMap(s -> s.sendMessage("**ATUALIZAÇÃO DE TICKET:** Seu ticket número " + t.getNumber() + " foi fechado por " + author.getName()))
				.queue(null, Helper::doNothing);

		TicketDAO.updateTicket(t);
	}
}
