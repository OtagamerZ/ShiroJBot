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

public class RollCommand extends Command {

	public RollCommand() {
		super("rrolar", new String[]{"rdado"}, "<função de dados>", "Rola um ou mais dados seguindo o padrão D&D.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			try {
				Actor.Player p = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId());
				channel.sendMessage("_**" + p.getCharacter().getName() + " rolou os dados:**_" + Utils.rollDice(String.join(" ", args), Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getStatus())).queue();
			} catch (Exception e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		} else if (Main.getInfo().getGames().get(guild.getId()).getMaster().equals(author.getId())) {
			try {
				channel.sendMessage(Utils.rollDice(String.join(" ", args), null)).queue();
			} catch (Exception e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}
	}
}
