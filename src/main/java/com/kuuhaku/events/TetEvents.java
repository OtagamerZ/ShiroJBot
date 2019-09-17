/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.persistence.NoResultException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TetEvents extends ListenerAdapter {

	@Override//removeGuildFromDB
	public void onGuildJoin(GuildJoinEvent event) {
		SQLite.addGuildToDB(event.getGuild());
		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getTet().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		guildConfig gc = new guildConfig();
		gc.setGuildId(event.getGuild().getId());
		SQLite.removeGuildFromDB(gc);
		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getTet().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		if (Main.getInfo().isReady()) {
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
						prefix = SQLite.getGuildPrefix(guild.getId());
					} catch (NoResultException | NullPointerException ignore) {
					}
				} else prefix = Main.getInfo().getDefaultPrefix();

				String rawMsgNoPrefix = rawMessage;
				String commandName = "";
				if (rawMessage.toLowerCase().startsWith(prefix)) {
					rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
					commandName = rawMsgNoPrefix.split(" ")[0].trim();
				}

				if (!commandName.equalsIgnoreCase("rpg")) return;

				boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
				String[] args = new String[]{};
				if (hasArgs) {
					args = Arrays.copyOfRange(rawMsgNoPrefix.split(" "), 1, rawMsgNoPrefix.split(" ").length);
				}

				boolean found = false;
				if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
					return;
				}
				for (Command command : Main.getCommandManager().getCommands()) {
					if (command.getName().equalsIgnoreCase(commandName)) {
						found = true;
					}
					for (String alias : command.getAliases()) {
						if (alias.equalsIgnoreCase(commandName)) {
							found = true;
						}
					}
					if (command.getCategory().isEnabled()) {
						found = false;
					}

					if (found) {
						Helper.logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild);
						if (Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
							command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
							Helper.spawnAd(channel);
							break;
						}
						try {
							channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
							Helper.spawnAd(channel);
							break;
						} catch (InsufficientPermissionException ignore) {
						}
					}
				}
			} catch (InsufficientPermissionException ignore) {

			} catch (ErrorResponseException e) {
				Helper.log(this.getClass(), LogLevel.ERROR, e.getErrorCode() + ": " + e + " | " + e.getStackTrace()[0]);
			}
		}
	}
}