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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class LinkTesterCommand extends Command {

	public LinkTesterCommand() {
		super("link", new String[]{"try"}, "<link>", "Testa um link para ver se ele consegue burlar a detecção de links.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | É necessário informar um link").queue();
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