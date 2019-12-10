package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL.VotesDAO;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class ListScoreCommand extends Command {

	public ListScoreCommand() {
		super("notas", new String[]{"scores"}, "Vota em um usuário.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		VotesDAO.getVotes(guild, message.getTextChannel());
	}
}