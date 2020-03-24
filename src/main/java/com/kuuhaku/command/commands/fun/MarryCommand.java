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
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.persistence.NoResultException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MarryCommand extends Command {

	public MarryCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MarryCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MarryCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MarryCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(":x: | Você precisa mencionar um usuário!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == author) {
				channel.sendMessage(":x: | Por mais que eu respeite seu lado otaku, você não pode se casar com sí mesmo!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser() && !author.getId().equals(Main.getInfo().getNiiChan())) {
				channel.sendMessage(":x: | Eu...já tenho alguém que estou de olho!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getJibril().getSelfUser() && !author.getId().equals(Main.getInfo().getNiiChan())) {
				try {
					TextChannel chn = Main.getJibril().getTextChannelById(channel.getId());
					assert chn != null;
					chn.sendMessage(":x: | Não tenho interesse em meros mortais!").queue();
				} catch (InsufficientPermissionException e) {
					channel.sendMessage(":x: | Ela não não tem como responder aqui, mas disse que não!").queue();
				}
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getTet().getSelfUser()) {
				try {
					TextChannel chn = Main.getTet().getTextChannelById(channel.getId());
					assert chn != null;
					chn.sendMessage(":x: | Nah, meus interesses são outros!").queue();
				} catch (InsufficientPermissionException e) {
					channel.sendMessage(":x: | Ele pediu pra dizer que não!").queue();
				}
				return;
			} else if (WaifuDAO.isWaifued(author.getId()) || WaifuDAO.isWaifued(message.getMentionedUsers().get(0).getId())) {
				channel.sendMessage(":x: | Essa pessoa já está casada, hora de passar pra frente!").queue();
				return;
			}

			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + ", deseja casar-se com " + author.getAsMention() + ", por toda eternidade (ou não) em troca de um bônus de XP?" +
					"\nDigite « Sim » para aceitar ou « Não » para negar.").queue();

			Main.getInfo().getAPI().addEventListener(new WaifuListener() {
				private final Consumer<Void> success = s -> Main.getInfo().getAPI().removeEventListener(this);
				private Future<?> timeout = channel.sendMessage("Visto que " + author.getAsMention() + " foi deixado no vácuo, vou me retirar e esperar um outro pedido.").queueAfter(5, TimeUnit.MINUTES, msg -> success.accept(null));

				@Override
				public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
					if (event.getAuthor().isBot() || event.getAuthor() != message.getMentionedUsers().get(0) || event.getChannel() != channel)
						return;

					Message msg = channel.retrieveMessageById(event.getMessageId()).complete();
					switch (msg.getContentRaw().toLowerCase()) {
						case "sim":
							channel.sendMessage("Eu os declaro husbando e waifu!").queue();
							com.kuuhaku.model.persistent.Member h = MemberDAO.getMemberById(author.getId() + guild.getId());
							com.kuuhaku.model.persistent.Member w = MemberDAO.getMemberById(message.getMentionedUsers().get(0).getId() + guild.getId());

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
