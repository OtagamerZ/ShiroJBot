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
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class AttackCommand extends Command {

	public AttackCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public AttackCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public AttackCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public AttackCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa informar o dano a ser causado ao jogador. Valores negativos irão curar.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(":x: | O valor deve ser numérico.").queue();
			return;
		} else if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa informar um usuário alvo.").queue();
			return;
		}

		int value = Integer.parseInt(args[0]);
		Actor.Player p = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
		Actor.Player t = Main.getInfo().getGames().get(guild.getId()).getPlayers().getOrDefault(message.getMentionedUsers().get(0).getId(), null);

		if (t == null) {
			channel.sendMessage(":x: | O alvo deve ser um jogador participante da campanha atual.").queue();
			return;
		}

		if (value > 0) {
			channel.sendMessage("_**" + p.getCharacter().getName() + " atacou " + t.getCharacter().getName() + ", causando " + value + " de dano.**_").queue();
			t.getCharacter().getStatus().damage(value);
		} else if (value < 0) {
			channel.sendMessage("_**" + p.getCharacter().getName() + " curou " + t.getCharacter().getName() + ", recuperando " + value + " pontos vida.**_").queue();
			t.getCharacter().getStatus().damage(value);
		} else {
			channel.sendMessage("_**Nada ocorreu.**_").queue();
		}
	}
}
