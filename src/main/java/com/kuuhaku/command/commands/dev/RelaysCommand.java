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

package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;

public class RelaysCommand extends Command {

	public RelaysCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public RelaysCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public RelaysCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public RelaysCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append("```\n");
		for (String s : Main.getRelay().getRelayMap().values()) {
			sb.append(s).append("\n");
		}
		sb.append("```\n");
		channel.sendMessage(sb.toString()).queue();
	}
}
