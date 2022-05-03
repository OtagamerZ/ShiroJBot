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
import com.kuuhaku.controller.postgresql.StaffDAO;
import com.kuuhaku.controller.postgresql.TicketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.StaffType;
import com.kuuhaku.model.persistent.Staff;
import com.kuuhaku.model.persistent.Ticket;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Command(
		name = "ticket",
		aliases = {"openticket", "tkt"},
		usage = "req_text",
		category = Category.INFO
)
@Requires({Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION})
@SlashGroup("suporte")
@SlashCommand(name = "ticket", args = {
		"{\"name\": \"texto\", \"description\": \"Conteúdo do ticket (min: 100 caractéres).\", \"type\": \"STRING\", \"required\": true}"
})
public class OpenTicketCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
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

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Deseja realmente abrir um ticket com o assunto `" + StringUtils.abbreviate(mensagem, 20) + "` (ele será enviado ao suporte)?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
							Main.getInfo().getConfirmationPending().remove(author.getId());

							int number = TicketDAO.openTicket(mensagem, member);

							EmbedBuilder eb = new EmbedBuilder()
									.setTitle("Ticket Nº " + number + "")
									.addField("Enviador por:", author.getAsTag() + " (" + guild.getName() + " | " + channel.getName() + ")", true)
									.addField("Enviado em:", Helper.FULL_DATE_FORMAT.format(message.getTimeCreated().atZoneSameInstant(ZoneId.of("GMT-3"))), true)
									.addField("Descrição:", "```" + mensagem + "```", false)
									.setFooter(author.getId())
									.setColor(Color.yellow);

							Ticket t = TicketDAO.getTicket(number);
							List<Staff> staff = StaffDAO.getStaff(StaffType.SUPPORT);
							Map<String, String> ids = new HashMap<>();
							for (Staff stf : staff) {
								try {
									Main.getInfo().getUserByID(stf.getUid()).openPrivateChannel()
											.flatMap(m -> m.sendMessageEmbeds(eb.build()))
											.flatMap(m -> {
												ids.put(stf.getUid(), m.getId());
												return m.pin();
											})
											.submit().get();
								} catch (ExecutionException | InterruptedException ignore) {
								}
							}

							author.openPrivateChannel()
									.flatMap(c -> c.sendMessage("**ATUALIZAÇÃO DE TICKET:** O número do seu ticket é " + number + ", você será atualizado sobre o progresso dele."))
									.queue(null, Helper::doNothing);

							t.setMsgIds(ids);
							TicketDAO.updateTicket(t);

							s.delete().queue(null, Helper::doNothing);
							channel.sendMessage(I18n.getString("str_successfully-opened-ticket")).queue();
						}), ShiroInfo.USE_BUTTONS, true, 60, TimeUnit.SECONDS,
						u -> u.getId().equals(author.getId()),
						ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
				));
	}

	@Override
	public String toCommand(SlashCommandEvent evt) {
		return Objects.requireNonNull(evt.getOption("texto")).getAsString();
	}
}
