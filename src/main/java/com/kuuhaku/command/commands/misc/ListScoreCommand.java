package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.VotesDAO;
import net.dv8tion.jda.api.entities.*;

public class ListScoreCommand extends Command {

	public ListScoreCommand() {
		super("notas", new String[]{"scores"}, "Mostra o ranking de votos de usuários.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		VotesDAO.getVotes(guild, message.getTextChannel());
	}
}