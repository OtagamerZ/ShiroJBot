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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.Locale;

@Command(
		name = "nomedodeck",
		aliases = {"deckname", "dname"},
		usage = "req_name-opt",
		category = Category.MISC
)
public class DeckNameCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		Deck dk = kp.getDeck();

		if (args.length == 0) {
			dk.setName(null);

			channel.sendMessage("✅ | Nome do deck atual limpo com sucesso.").queue();
		} else {
			if (!Helper.between(args[0].length(), 1, 21)) {
				channel.sendMessage("❌ | Nome inválido, ele deve ter entre 1 e 20 caractéres.").queue();
				return;
			}

			dk.setName(args[0].toLowerCase(Locale.ROOT));

			channel.sendMessage("✅ | Nome do deck atual definido com sucesso.").queue();
		}

		KawaiponDAO.saveKawaipon(kp);
	}
}
