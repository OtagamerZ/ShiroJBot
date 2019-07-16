package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.persistence.NoResultException;

public class TetEvents extends ListenerAdapter {

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (Main.getInfo().isReady()) {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			MessageChannel channel = message.getChannel();
			Guild guild = message.getGuild();
			String rawMessage = message.getContentRaw();

			String prefix = "";
			if (!Main.getInfo().isDev()) {
				try {
					prefix = SQLite.getGuildPrefix(guild.getId());
				} catch (NoResultException | NullPointerException ignore) {
				}
			} else prefix = Main.getInfo().getDefaultPrefix();

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

			if (message.getContentDisplay().equals(Main.getInfo().getSelfUser().getAsMention())) {
				channel.sendMessage("Para obter ajuda sobre como me utilizar use `" + prefix + "ajuda`.").queue();
				return;
			}

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().contains(prefix)) {
				rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
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
					if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
						channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
						Helper.spawnAd(channel);
						break;
					}
					if (command.getCategory() != Category.TET) return;
					command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
					Helper.spawnAd(channel);
					break;
				}
			}
		}
	}
}
