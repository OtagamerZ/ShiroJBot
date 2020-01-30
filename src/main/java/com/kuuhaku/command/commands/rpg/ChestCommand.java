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
import com.kuuhaku.handlers.games.rpg.entities.Equipped;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.exceptions.BadLuckException;
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;

public class ChestCommand extends Command {

	public ChestCommand() {
		super("rloot", new String[]{"rchest"}, "<@usuário> <baú>", "Roda os espólios de um baú e dá ao jogador mencionado.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(":x: | Você precisa especificar o jogador").queue();
				return;
			} else if (args.length < 2) {
				channel.sendMessage(":x: | O segundo argumento precisa ser o nome do baú").queue();
				return;
			}

			try {
				Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getInventory();
				Item dropped = Main.getInfo().getGames().get(guild.getId()).getChest(String.join(" ", Arrays.copyOfRange(args, 1, args.length))).dropLoot(Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getStatus().getLuck());
				inv.addItem(dropped);
				channel.sendMessage(Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId()).getCharacter().getName() + " ganhou " + dropped).queue();
			} catch (BadLuckException e) {
				channel.sendMessage("Que azar! Você não ganhou nenhum item!").queue();
			} catch (UnknownItemException e) {
				channel.sendMessage(":x: | Baú não encontrado").queue();
			} catch (RuntimeException e) {
				channel.sendMessage(":x: | Jogador inválido").queue();
			}
		}
	}
}
