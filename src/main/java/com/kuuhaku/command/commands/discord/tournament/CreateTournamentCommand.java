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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TournamentDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "novotorneio",
		aliases = {"newtournament"},
		usage = "req_name",
		category = Category.SUPPORT
)
@Requires({Permission.MESSAGE_ADD_REACTION})
public class CreateTournamentCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (argsAsText.length() < 5) {
			channel.sendMessage("❌ | O nome do torneio deve possuir ao menos 5 caractéres.").queue();
			return;
		}

		Tournament t = new Tournament(argsAsText);
		channel.sendMessage("Você está prestes a criar um novo torneio chamado `" + argsAsText + "`, deseja confirmar?").queue(
				s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
							TournamentDAO.save(t);

							s.delete().queue(null, MiscHelper::doNothing);
							channel.sendMessage("✅ | Torneio criado com sucesso (para iniciá-lo use `" + prefix + "liberarchaves`)!").queue();
						}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES
						, u -> u.getId().equals(author.getId())
				), MiscHelper::doNothing
		);
	}
}
