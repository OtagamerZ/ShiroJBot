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
import net.dv8tion.jda.api.requests.restaction.InviteAction;

import java.awt.*;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "suporte",
		aliases = {"support", "assist"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.CREATE_INSTANT_INVITE
})
public class RequestAssistCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getRequests().getOrDefault(guild.getId(), null) != null) {
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
		channel.sendMessage("Deseja realmente abrir um ticket com o assunto `SUPORTE PRESENCIAL` (isso criará um convite de uso único para este servidor)?").queue(s ->
				Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
					if (!ShiroInfo.getHashes().remove(hash)) return;
					Main.getInfo().getConfirmationPending().invalidate(author.getId());

					InviteAction ia = Helper.createInvite(guild);

					if (ia == null) {
						channel.sendMessage("❌ | Não encontrei nenhum canal que eu possa criar um convite aqui.").queue();
						return;
					}

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

					ia.setMaxUses(1).queue(i -> {
						Main.getInfo().getRequests().put(guild.getId(), i);
					});
					TicketDAO.setIds(number, ids);
					s.delete().queue(null, Helper::doNothing);
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_successfully-requested-assist")).queue();
				}), true, 60, TimeUnit.SECONDS, u -> u.getId().equals(author.getId()))
		);
	}
}
