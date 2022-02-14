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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "inscrever",
		aliases = {"join"},
		usage = "req_id-opt",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class JoinTournamentCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			List<List<Tournament>> chunks = Helper.chunkify(TournamentDAO.getOpenTournaments(), 10);
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Torneios abertos");

			for (List<Tournament> chunk : chunks) {
				eb.clearFields();

				for (Tournament tn : chunk) {
					eb.addField(
							"`ID: " + tn.getId() + "` | " + tn.getName(),
							"Jogadores: %s | Chave de %s".formatted(
									tn.getParticipants().size(),
									tn.getSize()
							),
							false
					);
				}

				pages.add(new InteractPage(eb.build()));
			}

			if (pages.isEmpty()) {
				channel.sendMessage("❌ | Não há nenhum torneio pendente.").queue();
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
			} else if (t.getLookup(author.getId()) != null) {
				channel.sendMessage("❌ | Você já se inscreveu nesse torneio.").queue();
				return;
			}

			channel.sendMessage("Você está prestes a inscrever-se no torneio `" + t.getName() + "`, deseja confirmar?").queue(
					s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
								t.register(author.getId());
								TournamentDAO.save(t);

								s.delete().queue(null, Helper::doNothing);
								channel.sendMessage("✅ | Inscrição realizada com sucesso! Você será notificado quando as chaves forem liberadas.").queue();
							}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
							, u -> u.getId().equals(author.getId())
					), Helper::doNothing
			);
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}