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
import com.kuuhaku.handlers.games.rpg.Utils;
import com.kuuhaku.handlers.games.rpg.actors.Actor;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;
import java.util.Arrays;

public class MoveCommand extends Command {

	public MoveCommand() {
		super("rmover", new String[]{"rmove"}, "<XY>", "Move seu personagem 1 coordenada em qualquer direção.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
				if (args.length == 0) {
					channel.sendMessage(":x: | Você precisa especificar as coordenadas.").queue();
					return;
				} else if (args[0].length() != 2) {
					channel.sendMessage(":x: | As coordenadas precisam ser concatenadas onde cada uma possui apenas 1 letra.").queue();
					return;
				}

				char[] coords = {args[0].charAt(0), args[0].charAt(1)};
				Integer[] numCoords = Utils.coordToArray(coords[0], coords[1]);

				if (numCoords[0] + numCoords[1] > 2) {
					channel.sendMessage(":x: | Você só pode andar uma casa por vez.").queue();
					return;
				}

				try {
					Actor.Player p = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
					channel.sendMessage("_**" + p.getCharacter().getName() + " move-se para " + Arrays.toString(coords) + "**_").queue();
					p.move(Main.getInfo().getGames().get(guild.getId()).getCurrentMap(), Utils.coordToArray(coords[0], coords[1]));
					Main.getInfo().getGames().get(guild.getId()).render(message.getTextChannel()).queue();
				} catch (IllegalArgumentException e) {
					channel.sendMessage(":x: | A coordenada deve conter apenas 2 letras, onde a primeira é maiúscula e a segunda minúscula, e deve estar dentro dos limites do mapa.").queue();
				}
			}
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
