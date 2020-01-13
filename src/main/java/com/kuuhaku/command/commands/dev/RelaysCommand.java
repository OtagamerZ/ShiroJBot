package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;

public class RelaysCommand extends Command {

	public RelaysCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public RelaysCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public RelaysCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public RelaysCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append("```\n");
		for (String s : Main.getRelay().getRelayMap().values()) {
			sb.append(s).append("\n");
		}
		sb.append("```\n");
		channel.sendMessage(sb.toString()).queue();
	}
}
