/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.VotesDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NonNls;

public class RateCommand extends Command {

	public RateCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RateCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RateCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RateCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 2 || message.getMentionedUsers().size() < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-vote")).queue();
			return;
		} else if (!MemberDAO.getMemberByMid(author.getId()).get(0).canVote()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_already-voted")).queue();
			return;
		} else if (message.getMentionedUsers().get(0) == author) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-vote-yourself")).queue();
			return;
		}

		switch (args[1]) {
			case "positivo", "pos" -> VotesDAO.voteUser(guild, author, message.getMentionedUsers().get(0), true);
			case "negativo", "neg" -> VotesDAO.voteUser(guild, author, message.getMentionedUsers().get(0), false);
			default -> {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_wrong-vote")).queue();
				return;
			}
		}

		try {
			message.delete().queue();
		} catch (InsufficientPermissionException ignore) {
		}

		channel.sendMessage("✅ | Obrigada, seu voto ajudará tanto os administradores deste servidor quanto meus administradores!").queue();
	}
}