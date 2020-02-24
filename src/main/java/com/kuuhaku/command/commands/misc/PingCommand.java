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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PingCommand extends Command {

	public PingCommand() {
		super("ping", "Ping", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		float fp = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024f / 1024f;
		float max = Runtime.getRuntime().totalMemory() / 1024f / 1024f;
		channel.sendMessage(":ping_pong: Pong! ")
				.flatMap(m -> m.editMessage(m.getContentRaw() + " " + Main.getInfo().getPing() + " ms!"))
				.flatMap(m -> m.editMessage(m.getContentRaw() + "\n:floppy_disk: " + BigDecimal.valueOf(fp).setScale(2, RoundingMode.HALF_EVEN) + "/" + BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_EVEN) + " MB!"))
				.queue();
	}
}
