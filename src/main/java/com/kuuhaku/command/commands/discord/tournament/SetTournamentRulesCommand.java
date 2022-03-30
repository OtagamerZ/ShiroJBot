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
import com.kuuhaku.controller.postgresql.TournamentDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.Rules;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import com.kuuhaku.utils.json.JSONObject;
import net.dv8tion.jda.api.entities.*;

import java.util.Locale;

@Command(
		name = "regrastorneio",
		usage = "req_id-json",
		category = Category.SUPPORT
)
public class SetTournamentRulesCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário informar o ID do torneio.").queue();
			return;
		} else if (!argsAsText.contains(";")) {
			channel.sendMessage("❌ | Para definir as regras, informe o ID do torneio e o JSON separados por ponto-e-virgula.").queue();
			return;
		}

		try {
			String[] r = argsAsText.split(";");

			Tournament t = TournamentDAO.getTournament(Integer.parseInt(r[0]));
			if (t == null) {
				channel.sendMessage("❌ | Torneio inexistente.").queue();
				return;
			}

			JSONObject custom = CollectionHelper.getOr(StringHelper.findJson(argsAsText.toLowerCase(Locale.ROOT)), new JSONObject());

			t.setCustomRules(new Rules(custom, true));
			TournamentDAO.save(t);

			channel.sendMessage("✅ | Regras definidas com sucesso!").queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}
