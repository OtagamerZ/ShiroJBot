/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.StorageUnit;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;
import java.io.InputStream;

@Command(
		name = "background",
		aliases = {"fundo", "bg"},
		usage = "req_link",
		category = Category.MISC
)
public class BackgroundCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(I18n.getString("err_no-image")).queue();
			return;
		} else if (Helper.containsAny(args[0], "google", "goo.gl")) {
			channel.sendMessage("❌ | Você pegou o link da **pesquisa do Google** bobo!").queue();
			return;
		}

		try {
			InputStream is = Helper.getImage(args[0]);
			byte[] bytes = is.readAllBytes();

			if (bytes.length > StorageUnit.B.convert(4, StorageUnit.MB)) {
				channel.sendMessage("❌ | Só são permitidas imagens de até 4 MB.").queue();
				return;
			}

			Account acc = AccountDAO.getAccount(author.getId());
			acc.setBackground(args[0]);
			AccountDAO.saveAccount(acc);
			if (args[0].contains("discordapp"))
				channel.sendMessage(":warning: | Imagens que utilizam o CDN do Discord (postadas no Discord) correm o risco de serem apagadas com o tempo, mas de todo modo: Imagem de fundo trocada com sucesso!").queue();
			else channel.sendMessage("✅ | Imagem de fundo trocada com sucesso!").queue();
		} catch (IOException | NullPointerException e) {
			channel.sendMessage(I18n.getString("err_invalid-image")).queue();
		}
	}
}
