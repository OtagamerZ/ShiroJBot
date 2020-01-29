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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.WaifuDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.NoResultException;

public class DivorceCommand extends Command {
	public DivorceCommand() {
		super("divorciar", new String[]{"separar", "divorce"}, "Se separa de sua waifu. Isso irá reduzir seu bônus de XP com futuras waifus.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			com.kuuhaku.model.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
			if (mb.getWaifu() == null || mb.getWaifu().equals("")) {
				channel.sendMessage(":x: | Você não possui uma waifu!").queue();
				return;
			}

			channel.sendMessage("Que pena, eu achava que iam durar por mais tempo!").queue();
			WaifuDAO.removeMemberWaifu(mb);
		} catch (NoResultException ignore) {
		}
	}
}
