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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.WaifuDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.persistence.NoResultException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MarryCommand implements Executable {

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
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		try {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
				return;
			} else if (message.getMentionedUsers().get(0) == author) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-marry-yourself")).queue();
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getSelfUser() && !author.getId().equals(ShiroInfo.getNiiChan())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-marry-shiro")).queue();
				return;
			} else if (message.getMentionedUsers().get(0) == Main.getJibril().getSelfUser() && !author.getId().equals(ShiroInfo.getNiiChan())) {
				try {
					TextChannel chn = Main.getJibril().getTextChannelById(channel.getId());
					assert chn != null;
					chn.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-marry-jibril")).queue();
				} catch (InsufficientPermissionException e) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_marry-the-answer-is-no")).queue();
				}
				return;
			} else if (WaifuDAO.isWaifued(author.getId()) || WaifuDAO.isWaifued(message.getMentionedUsers().get(0).getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_already-married")).queue();
				return;
			} else if (author.getId().equals(Main.getSelfUser().getId())) {
				channel.sendMessage("Ei, o que você acha que está fazendo ao me forçar a me casar? :rage:").queue();
				return;
			} else if (message.getMentionedUsers().get(0).isBot()) {
				channel.sendMessage("Isso é um bot, não acho que dê pra se casar com um!").queue();
				return;
			}

			channel.sendMessage("""
					%s, deseja casar-se com %s, por toda eternidade (ou não) em troca de um bônus de XP?
					Digite « Sim » para aceitar ou « Não » para negar.
					""".formatted(message.getMentionedUsers().get(0).getAsMention(), author.getAsMention())
			).queue();

			Main.getInfo().getShiroEvents().addHandler(guild, new SimpleMessageListener() {
				private final Consumer<Void> success = s -> close();
				private Future<?> timeout = channel.sendMessage("Visto que " + author.getAsMention() + " foi deixado no vácuo, vou me retirar e esperar um outro pedido.").queueAfter(5, TimeUnit.MINUTES, msg -> success.accept(null));

				@Override
				public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
					if (event.getAuthor().isBot() || event.getAuthor() != message.getMentionedUsers().get(0) || event.getChannel() != channel)
						return;

					if (WaifuDAO.isWaifued(author.getId()) || WaifuDAO.isWaifued(message.getMentionedUsers().get(0).getId())) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_already-married")).queue();
						return;
					}

					try {
						Message msg = event.getMessage();
						switch (msg.getContentRaw().toLowerCase()) {
							case "sim" -> {
								channel.sendMessage("Eu os declaro husbando e waifu!").queue();
								WaifuDAO.saveCouple(author, message.getMentionedUsers().get(0));
								success.accept(null);
								timeout.cancel(true);
								timeout = null;
							}
							case "não" -> {
								channel.sendMessage("Pois é, hoje não tivemos um casamento, que pena.").queue();
								success.accept(null);
								timeout.cancel(true);
								timeout = null;
							}
						}
					} catch (InsufficientPermissionException e) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-message-history-permission")).queue();
						success.accept(null);
						timeout.cancel(true);
						timeout = null;
					}
				}
			});
		} catch (NoResultException ignore) {
		}
	}
}
