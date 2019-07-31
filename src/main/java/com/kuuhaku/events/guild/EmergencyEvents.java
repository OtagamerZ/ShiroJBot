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

package com.kuuhaku.events.guild;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class EmergencyEvents extends ListenerAdapter {
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (Main.getInfo().isReady()) {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			MessageChannel channel = message.getChannel();
			Guild guild = message.getGuild();
			String rawMessage = message.getContentRaw();

			String prefix = Main.getInfo().getDefaultPrefix();

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

			if (message.getContentDisplay().equals(Main.getInfo().getSelfUser().getAsMention())) {
				channel.sendMessage("Para obter ajuda sobre como me utilizar use `" + prefix + "ajuda`.").queue();
				return;
			}

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().contains(prefix)) {
				rawMsgNoPrefix = rawMessage.replace(prefix, "").trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
			}

			boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
			String[] args = new String[]{};
			if (hasArgs) {
				args = rawMsgNoPrefix.substring(commandName.length()).trim().split(" ");
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
					try {
						if (Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
							command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
							Helper.spawnAd(channel);
							break;
						}
					} catch (Exception e) {
						channel.sendMessage(":warning: | Eu estou no modo de emergencia devido à queda do servidor, alguns comandos estão indisponíveis.").queue();
						return;
					}
					channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
					Helper.spawnAd(channel);
					break;
				}
			}
		}
	}
}