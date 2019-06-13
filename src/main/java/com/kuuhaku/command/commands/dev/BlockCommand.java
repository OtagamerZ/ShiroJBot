package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.RelayBlockList;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.sql.SQLException;

public class BlockCommand extends Command {

	public BlockCommand() {
		super("block", new String[]{"bloquear"}, "Bloqueia alguém de usar o chat global.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa passar o ID do usuário a ser bloqueado.").queue();
			return;
		} else if (args.length == 1) {
			channel.sendMessage(":x: | Você precisa passar o a razão para o bloqueio.").queue();
			return;
		} else if (args[1].equals("perma")) {
			RelayBlockList.permaBlockID(args[0]);
			channel.sendMessage("Usuário banido permanentemente do chat global.").queue();
			return;
		}

		RelayBlockList.blockID(args[0], String.join(" ", args).replace(args[0], "").trim());
		channel.sendMessage("Usuário bloqueado do chat global.").queue();
	}
}
