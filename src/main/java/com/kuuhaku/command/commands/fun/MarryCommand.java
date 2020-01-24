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

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.MemberDAO;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.NoResultException;

public class MarryCommand extends Command {
	public MarryCommand() {
		super("casar", new String[]{"declarar", "marry"}, "<@usuário>", "Pede um usuário em casamento.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(":x: | Você precisa mencionar um usuário!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == author) {
				channel.sendMessage(":x: | Por mais que eu respeite seu lado otaku, você não pode se casar com sí mesmo!").queue();
				return;
			} else if (!MemberDAO.getMemberById(author.getId() + guild.getId()).getWaifu().isEmpty() || !MemberDAO.getMemberById(message.getMentionedUsers().get(0).getId() + guild.getId()).getWaifu().isEmpty()) {
				channel.sendMessage(":x: | Essa pessoa já está casada, hora de passar pra frente!").queue();
				return;
			}

			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + ", deseja casar-se com " + author.getAsMention() + ", por toda eternidade ou até que meu Nii-chan crie um comando de divórcio?" +
					"\nDigite `SIM` para aceitar ou `NÃO` para negar.").queue();
			Main.getInfo().getQueue().add(new User[]{author, message.getMentionedUsers().get(0)});
		} catch (NoResultException ignore) {
		}
	}
}
