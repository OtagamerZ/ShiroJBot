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

package com.kuuhaku.command.commands.rpg;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class SetMaxPointsCommand extends Command {

	public SetMaxPointsCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public SetMaxPointsCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public SetMaxPointsCommand(@NonNls String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public SetMaxPointsCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
			if (args.length == 0) {
				channel.sendMessage(":x: | É necessário informar a quantidade máxima de pontos.").queue();
				return;
			} else if (!StringUtils.isNumeric(args[0])) {
				channel.sendMessage(":x: | A quantidade deve ser um valor inteiro.").queue();
				return;
			}

			Main.getInfo().getGames().get(guild.getId()).setMaxPts(Integer.parseInt(args[0]));
		}
	}
}
