package com.kuuhaku.command.commands.owner;

import com.kuuhaku.Main;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;

import java.sql.SQLException;

public class KillCommand extends Command {
	
	public KillCommand() {
		super("kill", new String[] {"shutdown"}, "Mata a Shiro!", Category.OWNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if(author.getId().equals(Main.getInfo().getNiiChan())) {
			channel.sendMessage("Sayonara, Nii-chan! <3").queue();
		} else {
			channel.sendMessage("Iniciando o protocolo de encerramento...").queue();
		}

		try {
			Main.shutdown();
		} catch (SQLException err) { err.printStackTrace(); }
		System.exit(0);
	}
}
