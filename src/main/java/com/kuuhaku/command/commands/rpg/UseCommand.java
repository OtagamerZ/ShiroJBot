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
import com.kuuhaku.handlers.games.rpg.actors.Actor;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.enums.Equipment;
import com.kuuhaku.handlers.games.rpg.enums.Resource;
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class UseCommand extends Command {

	public UseCommand() {
		super("rusar", new String[]{"ruse"}, "<item/ouro> [qtd de ouro]", "Utiliza um item ou uma quantidade de ouro.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 2 && Helper.containsAny(args[0], Resource.MONEY.getAliases())) {
			channel.sendMessage(":x: | Você precisa especificar a quantia de ouro a ser gasta.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[1])) {
			channel.sendMessage(":x: | A quantia de ouro deve ser numérica.").queue();
			return;
		}

		Actor.Player p = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
		if (Helper.containsAny(args[0], Resource.MONEY.getAliases())) {
			if (p.getCharacter().getInventory().getGold() < Integer.parseInt(args[0])) {
				channel.sendMessage(":x: | Você não possui essa quantia de ouro.").queue();
				return;
			}

			p.getCharacter().getInventory().addGold(-Integer.parseInt(args[1]));
			channel.sendMessage("_**" + p.getCharacter().getName() + " gastou " + args[1] + " moedas.**_").queue();
		} else {
			try {
				Item i = p.getCharacter().getInventory().getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				if (i.getType() != Equipment.MISC) {
					channel.sendMessage(":x: | Você só pode usar itens do tipo não-equipável.").queue();
					return;
				}
				p.getCharacter().getInventory().removeItem(i);
				channel.sendMessage("_**" + p.getCharacter().getName() + " usou um(a) " + i.getName() + ".**_").queue();
			} catch (UnknownItemException e) {
				channel.sendMessage(":x: | Você não possui este item.").queue();
			}
		}
	}
}
