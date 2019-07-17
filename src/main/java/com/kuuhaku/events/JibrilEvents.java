package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.persistence.NoResultException;

public class JibrilEvents extends ListenerAdapter {

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (Main.getRelay().getRelayMap().containsValue(event.getChannel().getId()) && !event.getAuthor().isBot()) {
			if (RelayBlockList.check(event.getAuthor().getId())) {
				event.getMessage().delete().queue();
				event.getAuthor().openPrivateChannel().queue(c -> c.sendMessage(":x: | Você não pode mandar mensagens no chat global (bloqueado).").queue());
				return;
			} else if (event.getMessage().getContentRaw().length() < 5) {
				event.getAuthor().openPrivateChannel().queue(c -> c.getMessageById(c.getLatestMessageId()).queue(m -> {
					String s = ":warning: | Cuidado, mensagens de SPAM podem fazer com que você seja bloqueado do chat global (desconsiderar caso não tenha sido SPAM)!\nEsta mensagem não aparecerá novamente enquanto for a última mensagem.";
					if (!m.getContentRaw().equals(s)) c.sendMessage(s).queue();
				}));
			}
			String[] msg = event.getMessage().getContentRaw().split(" ");
			for (int i = 0; i < msg.length; i++) {
				try {
					if (Helper.findURL(msg[i]) && !MySQL.getTagById(event.getAuthor().getId()).isVerified())
						msg[i] = "`LINK BLOQUEADO`";
				} catch (NoResultException e) {
					if (Helper.findURL(msg[i])) msg[i] = "`LINK BLOQUEADO`";
				}
			}
			if (String.join(" ", msg).length() < 2048) {
				try {
					if (MySQL.getTagById(event.getAuthor().getId()).isVerified() && event.getMessage().getAttachments().size() > 0) {
						try {
							Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), event.getMessage().getAttachments().get(0).getUrl());
						} catch (Exception e) {
							Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), null);
						}
						return;
					}
					Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), null);
				} catch (NoResultException e) {
					Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), null);
				}
			} else event.getChannel().sendMessage(":x: | Mensagem muito longa! (Max. 2048 letras)").queue();
		}

		else if (Main.getInfo().isReady()) {
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

			Helper.battle(event);

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
					if (!command.getCategory().equals(Category.PARTNER)) return;
					command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
					Helper.spawnAd(channel);
					break;
				}
			}
		}
	}
}
