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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.WaifuDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.NoResultException;

@Command(
		name = "divorciar",
		aliases = {"separar", "divorce"},
		category = Category.FUN
)
public class DivorceCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		try {
			if (!WaifuDAO.isWaifued(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_divorce-no-waifu")).queue();
				return;
			}

			channel.sendMessage("Que pena, eu achava que iam durar por mais tempo!").queue();
			WaifuDAO.removeCouple(author);
		} catch (NoResultException ignore) {
		}
	}
}
