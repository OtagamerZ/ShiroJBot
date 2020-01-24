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

import java.util.Arrays;

public class RemoveCommand extends Command {

	public RemoveCommand() {
		super("rremover", new String[]{"rremove"}, "<tipo> <@usuário/nome>", "Remove um registro. Os tipos são **player**, **monster**, **item** ou **map**.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (args.length < 2) {
				channel.sendMessage(":x: | É necessário especificar o tipo de registro e o nome do registro").queue();
				return;
			}
			try {
				switch (args[0].toLowerCase()) {
					case "p":
					case "player":
						if (message.getMentionedUsers().size() == 0) {
							channel.sendMessage(":x: | É necessário especificar o jogador por meio de menção").queue();
							return;
						}
						Main.getInfo().getGames().get(guild.getId()).getPlayers().remove(message.getMentionedUsers().get(0).getId());
						channel.sendMessage("Jogador excluído com sucesso.").queue();
						break;
					case "m":
					case "monster":
						Main.getInfo().getGames().get(guild.getId()).getMonsters().remove(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
						channel.sendMessage("Monstro excluído com sucesso.").queue();
						break;
					case "i":
					case "item":
						Main.getInfo().getGames().get(guild.getId()).getPlayers().forEach((k, v) -> {
							try {
								v.getCharacter().getInventory().unequip(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
								v.getCharacter().getInventory().removeItem(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
							} catch (UnknownItemException ignore) {
							}
						});
						Main.getInfo().getGames().get(guild.getId()).getItems().remove(Main.getInfo().getGames().get(guild.getId()).getItem(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
						channel.sendMessage("Item excluído com sucesso.").queue();
						break;
					case "w":
					case "map":
						Main.getInfo().getGames().get(guild.getId()).getMaps().remove(Integer.parseInt(args[1]));
						channel.sendMessage("Mapa excluído com sucesso.").queue();
						break;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				channel.sendMessage(":x: | Índice maior que a lista.").queue();
			} catch (UnknownItemException e) {
				channel.sendMessage(":x: | Item inexistente").queue();
			} catch (Exception e) {
				channel.sendMessage(":x: | Cadastro inexistente.").queue();
			}
		}
	}
}
