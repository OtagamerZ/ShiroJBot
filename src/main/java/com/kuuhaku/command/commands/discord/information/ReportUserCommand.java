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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TicketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "report",
		aliases = {"reportar"},
		usage = "req_user-reason",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
public class ReportUserCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {

		if (message.getMentionedUsers().size() < 1) {
			channel.sendMessage(I18n.getString("err_mention-required")).queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(I18n.getString("err_report-reason-required")).queue();
			return;
		}

		String mensagem = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();

		if (mensagem.length() > 1000) {
			channel.sendMessage("❌ | Mensagem muito longa, por favor tente ser mais breve.").queue();
			return;
		}

		int number = TicketDAO.openTicket(mensagem, author);

		EmbedBuilder eb = new EmbedBuilder()
				.setTitle("Relatório de report (Ticket Nº " + number + ")")
				.addField("Enviador por:", author.getAsTag() + " (" + guild.getName() + " | " + channel.getName() + ")", true)
				.addField("Enviado em:", Helper.fullDateFormat.format(message.getTimeCreated().atZoneSameInstant(ZoneId.of("GMT-3"))), true)
				.addField("Usuário reportado:", message.getMentionedUsers().get(0).getAsTag(), true)
				.addField("Relatório:", "```" + mensagem + "```", false)
				.setFooter(author.getId())
				.setColor(Color.red);

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Deseja realmente abrir um ticket com o assunto `DENUNCIAR USUÁRIO`?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());

							Map<String, String> ids = new HashMap<>();
							for (String dev : ShiroInfo.getStaff()) {
								Main.getInfo().getUserByID(dev).openPrivateChannel()
										.flatMap(m -> m.sendMessage(eb.build()))
										.flatMap(m -> {
											ids.put(dev, m.getId());
											return m.pin();
										})
										.complete();
							}

							author.openPrivateChannel()
									.flatMap(c -> c.sendMessage("**ATUALIZAÇÃO DE TICKET:** O número do seu ticket é " + number + ", você será atualizado do progresso dele."))
									.queue(null, Helper::doNothing);
							TicketDAO.setIds(number, ids);
							s.delete().queue(null, Helper::doNothing);
							channel.sendMessage(I18n.getString("str_successfully-reported-user")).queue();
						}), true, 60, TimeUnit.SECONDS,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}
}
