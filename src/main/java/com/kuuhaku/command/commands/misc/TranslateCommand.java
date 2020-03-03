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
import com.kuuhaku.controller.Tradutor;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Arrays;

public class TranslateCommand extends Command {

	public TranslateCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public TranslateCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public TranslateCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public TranslateCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage(":x: | Você precisa especificar de qual idioma e para qual idioma devo traduzir o texto.").queue();
			return;
		} else if (!args[0].contains(">")) {
			channel.sendMessage(":x: | O primeiro argumento deve ser de qual e para qual idioma devo traduzir (`de>para`).").queue();
			return;
		}

		String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		String[] fromTo = args[0].split(">");

		try {
			channel.sendMessage("**Traduzido de " + fromTo[0] + " para " + fromTo[1] + "**\n" + Tradutor.translate(fromTo[0], fromTo[1], text)).queue();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
