package com.kuuhaku.command.commands.dev;

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
			if (message.getMentionedUsers().size() == 0) {
				channel.sendMessage(":x: | Você precisa passar o ID do usuário a ser bloqueado.").queue();
				return;
			} else if (args.length == 1) {
				channel.sendMessage(":x: | Você precisa passar o a razão para o bloqueio.").queue();
				return;
			} else if (args[1].equals("perma")) {
				RelayBlockList.permaBlockID(message.getMentionedUsers().get(0).getId());
				channel.sendMessage("Usuário banido permanentemente do chat global.").queue();
				return;
			}

			RelayBlockList.blockID(message.getMentionedUsers().get(0).getId(), String.join(" ", args).replace(args[1], "").trim());
			channel.sendMessage("Usuário bloqueado do chat global.").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | ID de usuário incorreto.").queue();
		}
	}
}
