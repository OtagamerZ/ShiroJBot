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
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TournamentDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "torneio",
		aliases = {"tournament"},
		usage = "req_id-opt",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class TournamentCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			List<List<Tournament>> chunks = Helper.chunkify(TournamentDAO.getTournaments(), 10);
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Torneios");

			for (List<Tournament> chunk : chunks) {
				eb.clearFields();

				for (Tournament tn : chunk) {
					eb.addField(
							"`ID: %d` | %s%s | Chave de %s".formatted(
									tn.getId(),
									tn.getName(),
									tn.isFinished() ? " (ENCERRADO)" : tn.isClosed() ? " (FECHADO)" : tn.getLookup(author.getId()) != null ? " (INSCRITO)" : "",
									tn.getSize()
							),
							StringUtils.abbreviate(tn.getDescription(), 100),
							false
					);
				}

				pages.add(new InteractPage(eb.build()));
			}

			if (pages.isEmpty()) {
				channel.sendMessage("❌ | Não há nenhum torneio ainda.").queue();
				return;
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(
					s -> Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES)
			);
			return;
		}

		try {
			Tournament t = TournamentDAO.getTournament(Integer.parseInt(args[0]));
			if (t == null) {
				channel.sendMessage("❌ | Torneio inexistente.").queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Torneio " + t.getName() + " (Chave de " + t.getSize() + ")")
					.setDescription(t.getDescription())
					.setColor(Helper.textToColor(t.getName()))
					.addField("Jogadores: " + t.getParticipants().size(), t.isClosed() ? "" : "O tamanho do torneio pode variar de acordo com a quantidade de participantes", false)
					.setImage("attachment://brackets.jpg");

			String rules = t.getCustomRules().toString();
			if (!rules.isBlank())
				eb.addField("Regras adicionais:", rules, false);

			if (t.isClosed())
				channel.sendMessageEmbeds(eb.build())
						.addFile(Helper.writeAndGet(t.view(), "brackets", "jpg"))
						.queue();
			else
				channel.sendMessageEmbeds(eb.build()).queue();
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}