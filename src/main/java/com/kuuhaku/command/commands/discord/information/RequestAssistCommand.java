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
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RequestAssistCommand implements Executable {

	public RequestAssistCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RequestAssistCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RequestAssistCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RequestAssistCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getRequests().contains(guild.getId())) {
			channel.sendMessage("❌ | Este servidor já possui um pedido de auxílio em aberto, aguarde até que um membro da equipe de suporte feche-o.").queue();
			return;
		}

		int number = TicketDAO.openTicket("Requisição de suporte presencial.", author);
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Requisição de auxílio (Ticket Nº " + number + ")");
		eb.addField("ID do servidor:", guild.getId(), true);
		eb.addField("Requisitado por:", author.getAsTag() + " (" + guild.getName() + " | " + channel.getName() + ")", true);
		eb.addField("Requisitado em:", Helper.dateformat.format(message.getTimeCreated().atZoneSameInstant(ZoneId.of("GMT-3"))), true);
		eb.setFooter(author.getId());
		eb.setColor(Color.cyan);

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Deseja realmente abrir um ticket com o assunto `SUPORTE PRESENCIAL`?").queue(s ->
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
					Main.getInfo().getRequests().add(guild.getId());
					TicketDAO.setIds(number, ids);
					s.delete().queue(null, Helper::doNothing);
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_successfully-requested-assist")).queue();
				}), true, 60, TimeUnit.SECONDS, u -> u.getId().equals(author.getId()))
		);
	}
}
