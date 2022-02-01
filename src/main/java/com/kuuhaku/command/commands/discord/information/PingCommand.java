/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.Manager;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.SlashCommand;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "ping",
		category = Category.INFO
)
@SlashCommand(name = "ping")
public class PingCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		int fp = (int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
		channel.getJDA().getRestPing()
				.flatMap(t -> channel.sendMessage("""
						:ping_pong: Pong! %s ms.
						:file_cabinet: %s ms.
						:floppy_disk: %s MB.
						""".formatted(t, Manager.ping(), fp))
				)
				.queue();
	}
}