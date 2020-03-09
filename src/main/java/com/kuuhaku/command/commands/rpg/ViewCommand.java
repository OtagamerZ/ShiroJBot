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
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class ViewCommand extends Command {

	public ViewCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ViewCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ViewCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ViewCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (args.length == 0) {
				channel.sendMessage(":x: | É necessário especificar o nome do item").queue();
				return;
			}


			if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
				Main.getInfo().getGames().get(guild.getId()).getItem(args[0]).info(message.getTextChannel()).queue();
				return;
			}

			Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory().getItem(args[0]).info(message.getTextChannel()).queue();
		} catch (UnknownItemException e) {
			channel.sendMessage(":x: | Nenhum item encontrado com esse nome").queue();
		}
	}
}
