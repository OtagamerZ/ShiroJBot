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

package com.kuuhaku.events;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.rpg.NewCampaignCommand;
import com.kuuhaku.command.commands.rpg.NewPlayerCommand;
import com.kuuhaku.controller.mysql.LogDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.handlers.games.rpg.actors.Actor;
import com.kuuhaku.model.persistent.Log;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.NoResultException;
import java.util.Arrays;
import java.util.Objects;

public class TetEvents extends ListenerAdapter {

	@Override//removeGuildFromDB
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		try {
			Helper.sendPM(Objects.requireNonNull(event.getGuild().getOwner()).getUser(), "Obrigado por me adicionar ao seu servidor, utilize `s!help` e selecione a categoria `RPG` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!");
		} catch (Exception err) {
			TextChannel dch = event.getGuild().getDefaultChannel();
			if (dch != null) {
				if (dch.canTalk()) {
					dch.sendMessage("Obrigado por me adicionar ao seu servidor, utilize `s!help` e selecione a categoria `RPG` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!").queue();
				}
			}
		}

		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getTet().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getTet().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
		Main.getInfo().getShiroEvents().onPrivateMessageReceived(event);
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		try {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			MessageChannel channel = message.getChannel();
			Guild guild = message.getGuild();
			String rawMessage = message.getContentRaw();
			assert member != null;

			if (author.isBot()) return;

			String prefix = "";
			if (!Main.getInfo().isDev()) {
				try {
					prefix = GuildDAO.getGuildById(guild.getId()).getPrefix();
				} catch (NoResultException | NullPointerException ignore) {
				}
			} else prefix = Main.getInfo().getDefaultPrefix();

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().startsWith(prefix)) {
				rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
			}

			if (rawMessage.trim().equals("<@" + Main.getTet().getSelfUser().getId() + ">") || rawMessage.trim().equals("<@!" + Main.getTet().getSelfUser().getId() + ">")) {
				channel.sendMessage("Opa, eae jogador! Meus comandos são listados pela Shiro, digite `" + prefix + "help` e clique na categoria `RPG` para vê-los!").queue();
				return;
			} else if (rawMessage.startsWith("-") && Main.getInfo().getGames().containsKey(guild.getId()) && Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
				Actor.Player player = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());

				WebhookClientBuilder wcb = new WebhookClientBuilder(Objects.requireNonNull(Helper.getOrCreateWebhook((TextChannel) channel, "Tet", Main.getTet())).getUrl());
				WebhookClient client = wcb.build();

				String state = Helper.containsAll(rawMessage, "{", "}") ? rawMessage.substring(rawMessage.indexOf("{") + 1, rawMessage.indexOf("}")) : null;
				String quote = rawMessage.replace("{", "");

				if (state != null) {
					quote = quote.replaceFirst(".*(disse).*", "");
				}

				WebhookMessageBuilder wmb = new WebhookMessageBuilder();
				wmb.setUsername(player.getCharacter().getName());
				wmb.setAvatarUrl(player.getCharacter().getImage());
				wmb.setContent("**" + player.getCharacter().getName() + " " + (state == null ? "disse" : state) + ":** _" + (state == null ? quote.substring(1) : quote.split("}")[1].substring(1)) + "_");

				try {
					client.send(wmb.build());
					message.delete().queue();
				} catch (InsufficientPermissionException ignore) {
				}

				client.close();
			}

			boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
			String[] args = new String[]{};
			if (hasArgs) {
				args = Arrays.copyOfRange(rawMsgNoPrefix.split(" "), 1, rawMsgNoPrefix.split(" ").length);
			}

			boolean found = false;
			if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
				return;
			}

			for (Command command : Main.getRPGCommandManager().getCommands()) {
				found = JDAEvents.isFound(GuildDAO.getGuildById(guild.getId()), guild, commandName, found, command);

				if (found) {
					LogDAO.saveLog(new Log().setGuild(guild.getName()).setUser(author.getAsTag()).setCommand(rawMessage));
					Helper.logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild);
					if (Main.getInfo().getGames().get(guild.getId()) == null) {
						if (command.getClass() == NewCampaignCommand.class) {
							if (JDAEvents.checkPermissions(author, member, message, channel, guild, prefix, rawMsgNoPrefix, args, command))
								break;
							return;
						}
						channel.sendMessage(":x: | Este servidor ainda não possui uma campanha ativa.").queue();
						break;
					} else {
						if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId()) || (!Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId()) && command.getClass() == NewPlayerCommand.class) || Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
							if (JDAEvents.checkPermissions(author, member, message, channel, guild, prefix, rawMsgNoPrefix, args, command))
								break;
						} else {
							channel.sendMessage(":x: | Você ainda não criou um personagem, use o comando `" + prefix + "rnovo` para criar.").queue();
							break;
						}
					}
					if (JDAEvents.checkPermissions(author, member, message, channel, guild, prefix, rawMsgNoPrefix, args, command))
						break;
				}
			}
		} catch (InsufficientPermissionException ignore) {
		} catch (ErrorResponseException e) {
			Helper.logger(this.getClass()).error(e.getErrorCode() + ": " + e + " | " + e.getStackTrace()[0]);
		}
	}
}
