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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.persistence.NoResultException;

public class TheAnswerCommand implements Executable {

	public TheAnswerCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TheAnswerCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TheAnswerCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TheAnswerCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (guild.getId().equals("421495229594730496")) {
			try {
				if (TagDAO.getTagById(author.getId()).isReader())
					channel.sendMessage("❌ | Você já descobriu a resposta, não precisa mais usar este comando.").queue();
			} catch (NoResultException e) {
				TagDAO.addUserTagsToDB(author.getId());
			} finally {
				message.delete().queue();
				if (!TagDAO.getTagById(author.getId()).isReader()) {
					if (String.join(" ", args).replace(".", "").equalsIgnoreCase(System.getenv("SECRET"))) {
						TagDAO.giveTagReader(author.getId());
						channel.sendMessage("Obrigada por ler as regras!").queue();
					} else {
						channel.sendMessage("❌ | Resposta errada, leia as regras para achar a resposta.").queue();
					}
				}
			}
		} else
			channel.sendMessage("❌ | Este comando só pode ser usado no servidor " + ShiroInfo.getSupportServerName() + ".").queue();
	}
}
