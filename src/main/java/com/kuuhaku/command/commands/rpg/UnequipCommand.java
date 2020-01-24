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
import net.dv8tion.jda.api.entities.*;

public class UnequipCommand extends Command {

	public UnequipCommand() {
		super("rdesequipar", new String[]{"runequip"}, "<item>", "Desequipa um item.", Category.RPG);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getPlayers().containsKey(author.getId())) {
			Equipped inv = Main.getInfo().getGames().get(guild.getId()).getPlayers().get(author.getId()).getCharacter().getInventory();
			inv.unequip(inv.getItem(String.join(" ", args)));
			channel.sendMessage("Item " + String.join(" ", args) + " desequipado.").queue();
		}
	}
}
