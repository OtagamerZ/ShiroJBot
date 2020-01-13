package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;

public class KillCommand extends Command {

	public KillCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public KillCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public KillCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public KillCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if(author.getId().equals(Main.getInfo().getNiiChan())) {
			channel.sendMessage("Sayonara, Nii-chan! <3").queue();
		} else {
			channel.sendMessage("Iniciando o protocolo de encerramento...").queue();
		}

		Main.shutdown();
		System.exit(0);
	}
}
