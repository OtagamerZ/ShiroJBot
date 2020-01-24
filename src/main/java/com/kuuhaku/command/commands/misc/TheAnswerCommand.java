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
import com.kuuhaku.controller.mysql.TagDAO;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.NoResultException;

public class TheAnswerCommand extends Command {

	public TheAnswerCommand() {
		super("arespostaé", new String[]{"theansweris", "responder", "answer"}, "Leu as regras?", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (guild.getId().equals("421495229594730496")) {
			try {
				if (TagDAO.getTagById(author.getId()).isReader())
					channel.sendMessage(":x: | Você já descobriu a resposta, não precisa mais usar este comando.").queue();
			} catch (NoResultException e) {
				TagDAO.addUserTagsToDB(author.getId());
			} finally {
				message.delete().queue();
				if (!TagDAO.getTagById(author.getId()).isReader()) {
					if (String.join(" ", args).replace(".", "").equalsIgnoreCase(System.getenv("SECRET"))) {
						TagDAO.giveTagReader(author.getId());
						channel.sendMessage("Obrigado por ler as regras!").queue();
					} else {
						channel.sendMessage(":x: | Resposta errada, leia as regras para achar a resposta.").queue();
					}
				}
			}
		} else channel.sendMessage(":x: | Este comando só pode ser usado no servidor OtagamerZ.").queue();
	}
}
