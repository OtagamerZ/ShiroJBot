package com.kuuhaku.command.commands.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.Objects;

public class LogCommand extends Command {

	public LogCommand() {
		super("log", "Recupera o log da Shiro!", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("Aqui está!").addFile(Objects.requireNonNull(LogCommand.class.getClassLoader().getResourceAsStream("logs/stacktrace.log")), "trace.log").queue();
	}
}
