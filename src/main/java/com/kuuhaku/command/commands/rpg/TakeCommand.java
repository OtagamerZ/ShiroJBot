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
import com.kuuhaku.handlers.games.rpg.entities.Equipped;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class TakeCommand extends Command {

	public TakeCommand() {
		super("rtirar", new String[]{"rpegar"}, "<@usuário> <item/ouro> [qtd de ouro]", "Tira um item ou dinheiro de outro jogador.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Actor.Player t = Main.getInfo().getGames().get(guild.getId()).getPlayers().getOrDefault(message.getMentionedUsers().get(0).getId(), null);

		if (t == null) {
			channel.sendMessage(":x: | O alvo deve ser um jogador participante da campanha atual.").queue();
			return;
		}

		if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
			if (Utils.noPlayerAlert(args, message, channel)) return;

			Equipped inv = t.getCharacter().getInventory();
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
				channel.sendMessage("_**" + t.getCharacter().getName() + " perdeu $" + args[2] + ",00.**_").queue();
				inv.addGold(-Integer.parseInt(args[2]));
			} else {
				Item i = Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				channel.sendMessage("_**" + t.getCharacter().getName() + " perdeu o item " + i.getName() + ".**_").queue();
				inv.removeItem(i);
			}
		} else if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			if (Utils.noPlayerAlert(args, message, channel)) return;

			Actor.Player p = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
			Equipped tInv = t.getCharacter().getInventory();
			if ((args[1].equalsIgnoreCase("gold") || args[1].equalsIgnoreCase("ouro"))) {
				channel.sendMessage("_**" + t.getCharacter().getName() + " perdeu $" + args[2] + ",00.**_\n.\n.\n_**" + p.getCharacter().getName() + " obteve $" + args[2] + ",00**_").queue();
				tInv.addGold(-Integer.parseInt(args[2]));
			} else {
				Item i = Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				channel.sendMessage("_**" + t.getCharacter().getName() + " perdeu o item " + i.getName() + "**_\n.\n.\n_**" + p.getCharacter().getName() + " obteve o item " + i.getName() + "**_").queue();
				tInv.removeItem(i);
			}
		}
	}

}
