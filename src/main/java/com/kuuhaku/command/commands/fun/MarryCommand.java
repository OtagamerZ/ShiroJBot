/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.WaifuDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.events.WaifuListener;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import javax.annotation.Nonnull;
import javax.persistence.NoResultException;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MarryCommand extends Command {
	public MarryCommand() {
		super("casar", new String[]{"declarar", "marry"}, "<@usuário>", "Pede um usuário em casamento. Isso lhe dará um bônus de XP quando estiver no mesmo servidor que ela/ele.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(":x: | Você precisa mencionar um usuário!").queue();
				return;
			} else if (message.getMentionedUsers().get(0).isBot()) {
				channel.sendMessage(":x: | Não acho que ele(a) vá aceitar seu pedido...").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == author) {
				channel.sendMessage(":x: | Por mais que eu respeite seu lado otaku, você não pode se casar com sí mesmo!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser() && !author.getId().equals(Main.getInfo().getNiiChan())) {
				channel.sendMessage(":x: | Eu...já tenho alguém que estou de olho!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getJibril().getSelfUser() && !author.getId().equals(Main.getInfo().getNiiChan())) {
				try {
					Objects.requireNonNull(Main.getJibril().getTextChannelById(channel.getId())).sendMessage(":x: | Não tenho interesse em meros mortais!").queue();
				} catch (InsufficientPermissionException ignore) {
				}
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getTet().getSelfUser()) {
				try {
					Objects.requireNonNull(Main.getTet().getTextChannelById(channel.getId())).sendMessage(":x: | Nah, meus interesses são outros!").queue();
				} catch (InsufficientPermissionException ignore) {
				}
				return;
			} else if (!MemberDAO.getMemberById(author.getId() + guild.getId()).getWaifu().isEmpty() || !MemberDAO.getMemberById(message.getMentionedUsers().get(0).getId() + guild.getId()).getWaifu().isEmpty()) {
				channel.sendMessage(":x: | Essa pessoa já está casada, hora de passar pra frente!").queue();
				return;
			}

			Main.getInfo().getAPI().addEventListener(new WaifuListener() {
				private final Consumer<Void> success = s -> {
					channel.sendMessage("Visto que " + author.getAsMention() + " foi deixado no vácuo, vou me retirar e esperar um outro pedido.").queue();
					Main.getInfo().getAPI().removeEventListener(this);
				};
				private Future<?> timeout = channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + ", deseja casar-se com " + author.getAsMention() + ", por toda eternidade (ou não) em troca de um bônus de XP?" +
						"\nDigite `SIM` para aceitar ou `NÃO` para negar.").queueAfter(10, TimeUnit.MINUTES, msg -> success.accept(null));

				@Override
				public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
					if (event.getAuthor().isBot() || event.getAuthor() != message.getMentionedUsers().get(0) || event.getChannel() != channel)
						return;

					Message msg = channel.retrieveMessageById(event.getMessageId()).complete();
					switch (msg.getContentRaw().toLowerCase()) {
						case "sim":
							channel.sendMessage("Eu os declaro husbando e waifu!").queue();
							com.kuuhaku.model.Member h = MemberDAO.getMemberById(author.getId() + guild.getId());
							com.kuuhaku.model.Member w = MemberDAO.getMemberById(message.getMentionedUsers().get(0).getId() + guild.getId());

							WaifuDAO.saveMemberWaifu(h, message.getMentionedUsers().get(0));
							h.marry(message.getMentionedUsers().get(0));

							WaifuDAO.saveMemberWaifu(w, author);
							w.marry(author);

							MemberDAO.updateMemberConfigs(h);
							MemberDAO.updateMemberConfigs(w);
							success.accept(null);
							timeout.cancel(true);
							timeout = null;
							break;
						case "não":
							channel.sendMessage("Pois é, hoje não tivemos um casamento, que pena.").queue();
							success.accept(null);
							timeout.cancel(true);
							timeout = null;
							break;
					}
				}
			});
		} catch (NoResultException ignore) {
		}
	}
}
