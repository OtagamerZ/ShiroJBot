/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.exceed;

import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.handlers.games.disboard.model.Disboard;
import com.kuuhaku.model.enums.I18n;
import net.dv8tion.jda.api.entities.*;

/*@Command(
		name = "disboard",
		aliases = {"exmap", "mapa"},
		category = Category.EXCEED
)
@Requires({Permission.MESSAGE_ATTACH_FILES})*/
public class DisboardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!ExceedDAO.hasExceed(author.getId())) {
			channel.sendMessage(I18n.getString("err_exceed-map-no-exceed")).queue();
			return;
		}

		Disboard.view(channel);
	}
}
