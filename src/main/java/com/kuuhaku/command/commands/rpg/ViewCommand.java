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

public class ViewCommand extends Command {

	public ViewCommand() {
		super("rver", new String[]{"rinfo"}, "<item>", "Vê a descrição de um item", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (args.length == 0) {
				channel.sendMessage(":x: | É necessário especificar o nome do item").queue();
				return;
			} else if (args[0].equals("json")) {
				System.out.println(Main.getInfo().getGames().get(guild.getId()).getAsJSON());
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
