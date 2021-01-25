/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class LinkTesterCommand extends Command {

	public LinkTesterCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public LinkTesterCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public LinkTesterCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public LinkTesterCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | É necessário informar um link").queue();
			return;
		}

		String link = String.join(" ", args);

		String[] quotes = new String[]{
				"Peguei esse!",
				"Opa, hoje não!",
				"Quase!",
				"To ficando boa nisso!",
				"Ha! Esse já conheço",
				"Peguei no flagra!"
		};

		if (Helper.findURL(link)) {
			channel.sendMessage(quotes[
					(int) Helper.clamp(Math.round(quotes.length * Math.random()), 0, quotes.length - 1)
					]).queue();
		} else {
			channel.sendMessage("Não detectei nenhum link nesta mensagem. Caso seja um link, por favor use o comando abaixo para informar meus desenvolvedores!\n`s!bug Link não detectado - " + link + "`").queue();
		}
	}
}