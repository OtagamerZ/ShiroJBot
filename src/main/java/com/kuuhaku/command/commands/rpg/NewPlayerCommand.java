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
import com.kuuhaku.handlers.games.rpg.handlers.PlayerRegisterHandler;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class NewPlayerCommand extends Command {

	public NewPlayerCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public NewPlayerCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public NewPlayerCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public NewPlayerCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getGames().get(guild.getId()).getCurrentMap() == null) {
			channel.sendMessage(":x: | Ainda não existe nenhum mapa marcado como ativo, espere o mestre da campanha criá-lo").queue();
			return;
		}
		new PlayerRegisterHandler(Main.getInfo().getGames().get(guild.getId()).getCurrentMap(), message.getTextChannel(), Main.getTet(), author);
	}
}
