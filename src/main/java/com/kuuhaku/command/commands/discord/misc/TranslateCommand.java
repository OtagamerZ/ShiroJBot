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
import com.kuuhaku.command.Executable;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public class TranslateCommand implements Executable {

	public TranslateCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TranslateCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TranslateCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TranslateCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa especificar de qual idioma e para qual idioma devo traduzir o texto (`de>para`).").queue();
			return;
		} else if (!args[0].contains(">")) {
			channel.sendMessage("❌ | O primeiro argumento deve ser de qual e para qual idioma devo traduzir (`de>para`).").queue();
			return;
		}

		String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		String[] fromTo = args[0].split(">");

		if (fromTo.length < 2) {
			channel.sendMessage("❌ | Você precisa especificar de qual idioma e para qual idioma devo traduzir o texto (`de>para`).").queue();
			return;
		}

		/*try {
			channel.sendMessage("**Traduzido de " + fromTo[0] + " para " + fromTo[1] + "**\n" + Tradutor.translate(fromTo[0], fromTo[1], text)).queue();
		} catch (IOException e) {
			channel.sendMessage("❌ | Uma das duas linguagens é inválida, a lista completa de linguagens suportadas pode ser encontrada no link abaixo.\nhttps://tech.yandex.com/translate/doc/dg/concepts/api-overview-docpage/").queue();
		}*/
	}
}
