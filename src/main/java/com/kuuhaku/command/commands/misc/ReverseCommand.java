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
import net.dv8tion.jda.api.entities.*;

public class ReverseCommand extends Command {

	public ReverseCommand() {
		super("reverse", new String[] {"inverter"}, "<texto>", "Inverte o texto fornecido.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa de indicar o texto que deseja inverter.").queue();
			return;
		}

		String txt = String.join(" ", args);

		txt = new StringBuilder(txt.trim()).reverse().toString();
		channel.sendMessage(txt).queue();
	}

}
