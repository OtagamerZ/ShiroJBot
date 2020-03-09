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
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.enums.Resource;
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public class TakeCommand extends Command {

	public TakeCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TakeCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TakeCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TakeCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Utils.noPlayerAlert(args, message, channel)) return;

		Actor.Player t = Main.getInfo().getGames().get(guild.getId()).getPlayers().getOrDefault(message.getMentionedUsers().get(0).getId(), null);

		if (t == null) {
			channel.sendMessage(":x: | O alvo deve ser um jogador participante da campanha atual.").queue();
			return;
		}

		if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
			if (Helper.containsAny(args[1], Resource.MONEY.getAliases())) {
				t.getCharacter().getInventory().addGold(-Integer.parseInt(args[2]));
				channel.sendMessage("_**" + t.getCharacter().getName() + " perdeu $" + args[2] + " moeda" + (Integer.parseInt(args[2]) != 1 ? "s" : "") + ".**_").queue();
			} else {
				Item i = Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				t.getCharacter().getInventory().removeItem(i);
				channel.sendMessage("_**" + t.getCharacter().getName() + " perdeu o item " + i.getName() + ".**_").queue();
			}
			return;
		}

		Actor.Player p = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
		try {
			if (Helper.containsAny(args[1], Resource.MONEY.getAliases())) {
				if (t.getCharacter().getInventory().getGold() < Integer.parseInt(args[1])) {
					channel.sendMessage(":x: | O alvo não possui essa quantia de ouro.").queue();
					return;
				}

				t.getCharacter().getInventory().addGold(-Integer.parseInt(args[2]));
				channel.sendMessage("_**" + p.getCharacter().getName() + " roubou $" + args[2] + " moeda" + (Integer.parseInt(args[2]) != 1 ? "s" : "") + " de " + t.getCharacter().getName() + "!**_").queue();
			} else {
				Item i = t.getCharacter().getInventory().getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				t.getCharacter().getInventory().removeItem(i);
				channel.sendMessage("_**" + p.getCharacter().getName() + " roubou o item " + i.getName() + " de " + t.getCharacter().getName() + "!**_").queue();
			}
		} catch (UnknownItemException e) {
			channel.sendMessage(":x: | O alvo não possui este item.").queue();
		}
	}

}
