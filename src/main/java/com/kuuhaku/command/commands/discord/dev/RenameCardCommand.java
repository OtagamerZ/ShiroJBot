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

package com.kuuhaku.command.commands.discord.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Card;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.util.Locale;

@Command(
		name = "renomear",
		aliases = {"rename"},
		usage = "req_oldname-newname-name",
		category = Category.DEV
)
public class RenameCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa especificar o nome atual e o novo nome para renomear.").queue();
			return;
		}

		String oldName = args[0].toUpperCase(Locale.ROOT);
		String newName = args[1].toUpperCase(Locale.ROOT);
		Card c = CardDAO.getCard(oldName);

		if (c == null) {
			channel.sendMessage("❌ | Não existe uma carta com esse nome.").queue();
			return;
		}

		File master = new File(System.getenv("CARDS_PATH") + "../cards-waifu2x");
		if (new File(master, args[1]).exists()) {
			channel.sendMessage("❌ | Já existe uma carta com esse nome.").queue();
			return;
		}

		File w2xFile = new File(master, oldName + ".png");
		if (!w2xFile.renameTo(new File(master, newName + ".png"))) {
			channel.sendMessage("❌ | Erro ao renomear o arquivo no diretório mestre.").queue();
			return;
		}

		File sorted = new File(System.getenv("CARDS_PATH") + c.getAnime().getName());
		File srtFile = new File(sorted, oldName + ".png");
		if (!srtFile.renameTo(new File(sorted, newName + ".png"))) {
			channel.sendMessage("❌ | Erro ao renomear o arquivo no diretório categorizado.").queue();
			return;
		}

		if (args.length > 2)
			CardDAO.setCardName(
					oldName,
					newName,
					argsAsText.replace(args[0], "").replace(args[1], "").trim()
			);
		else
			CardDAO.setCardName(
					oldName,
					newName
			);

		channel.sendMessage("Carta renomeada com sucesso!").queue();
	}
}
