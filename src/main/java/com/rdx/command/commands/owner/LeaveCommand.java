package com.rdx.command.commands.owner;

import com.rdx.command.Category;
import com.rdx.command.Command;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class LeaveCommand extends Command {

	private static JDA api;

	public LeaveCommand() {
		super("leave", new String[] {"sair"}, "<ID do servidor>", "Sai do servidor cujo o ID foi dado.", Category.OWNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		try {
			Guild g = api.getGuildById(rawCmd.split(" ")[1]);
			g.leave().queue();
			channel.sendMessage("Ok, já saí daquele servidor, Nii-chan!").queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue();
		}
		
	}

}
