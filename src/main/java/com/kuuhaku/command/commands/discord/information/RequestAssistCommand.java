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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.TicketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Ticket;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.InviteAction;

import java.awt.*;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Command(
		name = "suporte",
		aliases = {"support", "assist"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_ADD_REACTION,
		Permission.MANAGE_SERVER,
		Permission.CREATE_INSTANT_INVITE
})
@SlashGroup("suporte")
@SlashCommand(name = "presencial")
public class RequestAssistCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Deseja realmente abrir um ticket com o assunto `SUPORTE PRESENCIAL` (isso criará um convite de uso único para este servidor)?")
				.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());

							int number = TicketDAO.openTicket("Requisição de suporte presencial.", member);
							EmbedBuilder eb = new EmbedBuilder();

							eb.setTitle("Requisição de auxílio (Ticket Nº " + number + ")");
							eb.addField("ID do servidor:", guild.getId(), true);
							eb.addField("Requisitado por:", author.getAsTag() + " (" + guild.getName() + " | " + channel.getName() + ")", true);
							eb.addField("Requisitado em:", Constants.FULL_DATE_FORMAT.format(message.getTimeCreated().atZoneSameInstant(ZoneId.of("GMT-3"))), true);
							eb.setFooter(author.getId());
							eb.setColor(Color.cyan);

							InviteAction ia = MiscHelper.createInvite(guild);

							if (ia == null) {
								channel.sendMessage("❌ | Não encontrei nenhum canal que eu possa criar um convite aqui.").queue();
								return;
							}

							Ticket t = TicketDAO.getTicket(number);
							List<String> staff = ShiroInfo.getStaff();
							Map<String, String> ids = new HashMap<>();
							for (String dev : staff) {
								try {
									Main.getInfo().getUserByID(dev).openPrivateChannel()
											.flatMap(m -> m.sendMessageEmbeds(eb.build()))
											.flatMap(m -> {
												ids.put(dev, m.getId());
												return m.pin();
											})
											.submit().get();
								} catch (ExecutionException | InterruptedException ignore) {
								}
							}

							author.openPrivateChannel()
									.flatMap(c -> c.sendMessage("**ATUALIZAÇÃO DE TICKET:** O número do seu ticket é " + number + ", você será atualizado sobre o progresso dele."))
									.queue(null, MiscHelper::doNothing);

							t.setMsgIds(ids);

							try {
								t.setInvite(ia.setMaxUses(1).submit().get().getCode());
								TicketDAO.updateTicket(t);
							} catch (InterruptedException | ExecutionException ignore) {
							}

							s.delete().queue(null, MiscHelper::doNothing);
							channel.sendMessage(I18n.getString("str_successfully-requested-assist")).queue();
						}), Constants.USE_BUTTONS, true, 60, TimeUnit.SECONDS,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}
}
