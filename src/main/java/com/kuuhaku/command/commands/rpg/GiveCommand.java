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
import com.kuuhaku.handlers.games.rpg.entities.Equipped;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class GiveCommand extends Command {

	public GiveCommand() {
		super("rdar", new String[]{"rgive"}, "<@usuário> <item/ouro> [qtd de ouro]", "Dá um item ou dinheiro à outro jogador.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Utils.noPlayerAlert(args, message, channel)) return;

		if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
			if (args.length < 3 && (args[1].equalsIgnoreCase("xp") || args[1].equalsIgnoreCase("ouro"))) {
				channel.sendMessage(":x: | Você precisa especificar a quantidade de XP").queue();
				return;
			}

			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro")))
				inv.addGold(Integer.parseInt(args[2]));
			else if ((args[1].equalsIgnoreCase("xp")))
				Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getStatus().addXp(Integer.parseInt(args[2]));
			else inv.addItem(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
			return;
		}

		Equipped selfInv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory();
		Equipped targetInv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
		if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
			selfInv.addGold(-Integer.parseInt(args[2]));
			targetInv.addGold(Integer.parseInt(args[2]));
		} else {
			selfInv.removeItem(selfInv.getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
			targetInv.addItem(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
		}
	}
}
