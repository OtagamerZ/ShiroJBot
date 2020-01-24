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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class RestartCommand extends Command {

	public RestartCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public RestartCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public RestartCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public RestartCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if(author.getId().equals(Main.getInfo().getNiiChan())) {
			channel.sendMessage("Matane, Nii-chan! <3").queue();
		} else {
			channel.sendMessage("Iniciando o protocolo de reinicialização...").queue();
		}

		try {
			Main.getJibril().shutdown();
			Main.getInfo().getAPI().shutdown();
			Main.main(Main.getArgs());
		} catch (Exception e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
