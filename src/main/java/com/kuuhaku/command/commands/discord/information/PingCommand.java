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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import java.text.MessageFormat;

@Command(
		name = "ping",
		category = Category.INFO
)
public class PingCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		int fp = (int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
		long currTime = System.currentTimeMillis();
		channel.sendMessage(":ping_pong: Pong! ")
				.flatMap(m -> m.editMessage(m.getContentRaw() + """
						 %s ms!
						:floppy_disk: %s MB!
						:telephone: %s 
						""".formatted(
						System.currentTimeMillis() - currTime,
						fp,
						MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_listeners"), Main.getShiroShards().getShards().get(0).getEventManager().getRegisteredListeners().size())
				)))
				.queue();
	}
}
