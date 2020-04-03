/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.VotesDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

public class VoteCommand extends Command {

	public VoteCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public VoteCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public VoteCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public VoteCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 2 || message.getMentionedUsers().size() < 1) {
			channel.sendMessage(":x: | É necessário mencionar um usuário e o tipo de voto (positivo/negativo).").queue();
			return;
		} else if (!MemberDAO.getMemberByMid(author.getId()).get(0).canVote()) {
			channel.sendMessage(":x: | Você já votou hoje, cada usuário possui apenas um voto por dia.").queue();
			return;
		} else if (message.getMentionedUsers().get(0) == author) {
			channel.sendMessage(":x: | Você não pode votar em si mesmo.").queue();
			return;
		}

		switch (args[1]) {
			case "positivo":
			case "pos":
			case ":thumbsup:":
				VotesDAO.voteUser(guild, author, message.getMentionedUsers().get(0), true);
				break;
			case "negativo":
			case "neg":
			case ":thumbsdown:":
				VotesDAO.voteUser(guild, author, message.getMentionedUsers().get(0), false);
				break;
		}

		try {
			message.delete().queue();
		} catch (InsufficientPermissionException ignore) {
		}

		channel.sendMessage(":white_check_mark: | Obrigada, seu voto ajudará tanto os administradores deste servidor quanto meus administradores!").queue();
	}
}