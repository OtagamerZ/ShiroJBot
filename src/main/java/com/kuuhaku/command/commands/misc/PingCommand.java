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
import org.jetbrains.annotations.NonNls;

public class PingCommand extends Command {

	public PingCommand(@NonNls String name, String description, Category category) {
		super(name, description, category);
	}

	public PingCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public PingCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public PingCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		int fp = (int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
		int max = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
		channel.sendMessage(":ping_pong: Pong! ")
				.flatMap(m -> m.editMessage(m.getContentRaw() + " " + Main.getInfo().getPing() + " ms!"))
				.flatMap(m -> m.editMessage(m.getContentRaw() + "\n:floppy_disk: " + fp + "/" + max + " MB!"))
				.flatMap(m -> m.editMessage(m.getContentRaw() + "\n:telephone: " + Main.getInfo().getAPI().getEventManager().getRegisteredListeners().size() + " eventos!"))
				.queue();
	}
}
