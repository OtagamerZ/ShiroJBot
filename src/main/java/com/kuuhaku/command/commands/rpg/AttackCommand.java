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
import com.kuuhaku.handlers.games.rpg.handlers.CombatHandler;
import com.kuuhaku.handlers.games.rpg.handlers.PvPHandler;
import net.dv8tion.jda.api.entities.*;

public class AttackCommand extends Command {

	public AttackCommand() {
		super("rduelar", new String[]{"rattack", "rduel"}, "<@usuário>", "Inicia um duelo com um jogador.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getMaster() == author) {
			if (message.getMentionedUsers().size() == 0) {
				channel.sendMessage(":x: | Você precisa especificar um usuário para atacar").queue();
				return;
			} else if (args.length == 1) {
				channel.sendMessage(":x: | Você precisa especificar um monstro como atacante").queue();
				return;
			}

			Actor.Player player = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
			Actor.Monster mob = Main.getInfo().getGames().get(guild.getId()).getMonsters().get(args[1]);

			new CombatHandler(Main.getTet(), Main.getInfo().getGames().get(guild.getId()), message.getTextChannel(), player, mob);
			return;
		}
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa especificar um usuário para duelar").queue();
			return;
		}
		Actor.Player player = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
		Actor.Player target = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(message.getMentionedUsers().get(0).getId());

		if (player.getPos() != target.getPos()) {
			channel.sendMessage(":x: | Este jogador não está na mesma área que você").queue();
			return;
		}

		new PvPHandler(Main.getTet(), message.getTextChannel(), player, target);
	}
}
