package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class VoteCommand extends Command {

	public VoteCommand() {
		super("votar", new String[]{"vote"}, "<@usuário> <positivo/negativo>", "Vota em um usuário.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0 || message.getMentionedUsers().size() < 1) {
			channel.sendMessage(":x: | É necessário mencionar um usuário").queue();
			return;
		} else if (!SQLite.getMemberByMid(author.getId()).canVote()) {
			channel.sendMessage(":x: | Você já votou hoje, cada usuário possui apenas um voto por dia").queue();
			return;
		}

		switch (args[1]) {
			case "positivo":
			case "pos":
			case ":thumbsup:":
				MySQL.voteUser(guild, author, message.getMentionedUsers().get(0), true);
				break;
			case "negativo":
			case "neg":
			case ":thumbsdown:":
				MySQL.voteUser(guild, author, message.getMentionedUsers().get(0), false);
				break;
			default:
				channel.sendMessage(":x: | É necessário informar o tipo do voto (positivo ou negativo)").queue();
				return;
		}

		try {
			message.delete().queue();
		} catch (InsufficientPermissionException ignore) {
		}

		channel.sendMessage(":white_check_mark: | Obrigada, seu voto ajudará tanto os administradores deste servidor quanto meus administradores!").queue();
	}
}