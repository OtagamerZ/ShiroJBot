package com.kuuhaku.command.commands.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.File;

public class LogCommand extends Command {

	public LogCommand() {
		super("log", "Recupera o log da Shiro!", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		File log = new File("logs/stacktrace.log");
		if (log.exists()) channel.sendMessage("Aqui está!").addFile(log).queue();
		else channel.sendMessage(":x: | Arquivo de log não encontrado.").queue();
	}
}
