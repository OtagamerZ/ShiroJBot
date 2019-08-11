package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.RelayBlockList;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class BlockCommand extends Command {

	public BlockCommand() {
		super("block", new String[]{"bloquear"}, "Bloqueia alguém de usar o chat global.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			if (message.getMentionedUsers().size() > 0) {
				if (args.length == 1) {
					channel.sendMessage(":x: | Você precisa passar o a razão para o bloqueio.").queue();
					return;
				} else if (args[1].equals("perma")) {
					RelayBlockList.permaBlockID(message.getMentionedUsers().get(0).getId());
					Main.getRelay().relayMessage(message, message.getMentionedUsers().get(0).getAsMention() + " banido permanentemente do chat global.\nRazão: " + String.join(" ", args).substring(1), guild.getSelfMember(), guild, null);
					return;
				}

				RelayBlockList.blockID(message.getMentionedUsers().get(0).getId(), String.join(" ", args).replace(args[1], "").trim());
				Main.getRelay().relayMessage(message, message.getMentionedUsers().get(0).getAsMention() + " bloqueado do chat global.\nRazão: " + String.join(" ", args).substring(1), guild.getSelfMember(), guild, null);
			} else if (Main.getInfo().getUserByID(args[0]) != null) {
				if (args.length == 1) {
					channel.sendMessage(":x: | Você precisa passar o a razão para o bloqueio.").queue();
					return;
				} else if (args[1].equals("perma")) {
					RelayBlockList.permaBlockID(message.getMentionedUsers().get(0).getId());
					Main.getRelay().relayMessage(message, "<@" + args[0] + "> banido permanentemente do chat global.\nRazão: " + String.join(" ", args).substring(1), guild.getSelfMember(), guild, null);
					return;
				}

				RelayBlockList.blockID(message.getMentionedUsers().get(0).getId(), String.join(" ", args).replace(args[1], "").trim());
				Main.getRelay().relayMessage(message, "<@" + args[0] + "> bloqueado do chat global.\nRazão: " + String.join(" ", args).substring(1), guild.getSelfMember(), guild, null);
			} else {
				channel.sendMessage(":x: | Você precisa passar o ID do usuário a ser bloqueado.").queue();
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | ID de usuário incorreto.").queue();
		}
	}
}
