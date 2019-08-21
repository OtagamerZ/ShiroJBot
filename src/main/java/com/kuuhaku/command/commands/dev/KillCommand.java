package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class KillCommand extends Command {
	
	public KillCommand() {
		super("kill", new String[]{"shutdown"}, "Mata a Shiro!", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if(author.getId().equals(Main.getInfo().getNiiChan())) {
			channel.sendMessage("Sayonara, Nii-chan! <3").queue();
		} else {
			channel.sendMessage("Iniciando o protocolo de encerramento...").queue();
		}

		Main.shutdown();
		System.exit(0);
	}
}
