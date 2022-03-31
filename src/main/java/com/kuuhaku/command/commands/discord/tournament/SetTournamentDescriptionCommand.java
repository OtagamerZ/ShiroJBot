/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.tournament;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.tournament.Tournament;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "desctorneio",
		usage = "req_id-text",
		category = Category.SUPPORT
)
public class SetTournamentDescriptionCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário informar o ID do torneio.").queue();
			return;
		} else if (!argsAsText.contains(";")) {
			channel.sendMessage("❌ | Para adicionar uma descrição, informe o ID do torneio e um texto separados por ponto-e-virgula.").queue();
			return;
		}

		try {
			String[] r = argsAsText.split(";");

			Tournament t = Tournament.find(Tournament.class, Integer.parseInt(r[0]));
			if (t == null) {
				channel.sendMessage("❌ | Torneio inexistente.").queue();
				return;
			}

			t.setDescription(r[1]);
			t.save();

			channel.sendMessage("✅ | Descrição adicionada com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}
