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
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OpenTicketCommand implements Executable {

	public OpenTicketCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public OpenTicketCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public OpenTicketCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public OpenTicketCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {

		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa definir uma mensagem.").queue();
			return;
		}

		String mensagem = String.join(" ", args).trim();

		if (mensagem.length() > 1000) {
			channel.sendMessage("❌ | Mensagem muito longa, por favor tente ser mais breve.").queue();
			return;
		} else if (mensagem.length() < 100) {
			channel.sendMessage("❌ | Mensagem muito curta, por favor tente ser mais detalhado.").queue();
			return;
		}

		int number = TicketDAO.openTicket(mensagem, author);

		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Ticket Nº " + number + "");
		eb.addField("Enviador por:", author.getAsTag() + " (" + guild.getName() + " | " + channel.getName() + ")", true);
		eb.addField("Enviado em:", Helper.dateformat.format(message.getTimeCreated().atZoneSameInstant(ZoneId.of("GMT-3"))), true);
		eb.addField("Descrição:", "```" + mensagem + "```", false);
		eb.setFooter(author.getId());
		eb.setColor(Color.yellow);

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Deseja realmente abrir um ticket com o assunto `" + StringUtils.abbreviate(mensagem, 20) + "`?").queue(s ->
				Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
					if (!ShiroInfo.getHashes().remove(hash)) return;
					Main.getInfo().getConfirmationPending().invalidate(author.getId());

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
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_successfully-opened-ticket")).queue();
				}), true, 60, TimeUnit.SECONDS, u -> u.getId().equals(author.getId()))
		);
	}
}
