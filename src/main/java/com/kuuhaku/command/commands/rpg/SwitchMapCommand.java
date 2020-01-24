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

public class SwitchMapCommand extends Command {

	public SwitchMapCommand() {
		super("raomapa", new String[]{"rtrocarmapa", "rtomap"}, "<índice>", "Muda de mapa.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (args.length == 0) {
				channel.sendMessage(":x: | É necessário especificar o número do mapa").queue();
				return;
			}
			try {
				Main.getInfo().getGames().get(guild.getId()).switchMap(Integer.parseInt(args[0]));
			} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
				channel.sendMessage(":x: | Índice inválido, existem " + Main.getInfo().getGames().get(guild.getId()).getMaps().size() + " mapas cadastrados.").queue();
			}
		}
	}
}
