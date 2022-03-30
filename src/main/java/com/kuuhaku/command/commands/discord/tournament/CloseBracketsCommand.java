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
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TournamentDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.tournament.Participant;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "liberarchaves",
		aliases = {"closebrackets", "fecharchaves"},
		usage = "req_id-opt",
		category = Category.SUPPORT
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class CloseBracketsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			List<List<Tournament>> chunks = CollectionHelper.chunkify(TournamentDAO.getOpenTournaments(), 10);
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Torneios abertos");

			for (List<Tournament> chunk : chunks) {
				eb.clearFields();

				for (Tournament tn : chunk) {
					eb.addField(
							"`ID: " + tn.getId() + "` | " + tn.getName() + (tn.isClosed() ? " (FECHADO)" : ""),
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
					s -> Pages.paginate(s, pages, Constants.USE_BUTTONS, 1, TimeUnit.MINUTES)
			);
			return;
		}

		try {
			Tournament t = TournamentDAO.getTournament(Integer.parseInt(args[0]));
			if (t == null) {
				channel.sendMessage("❌ | Torneio inexistente.").queue();
				return;
			} else if (MathHelper.prcnt(t.getParticipants().size(), t.getSize()) < 0.75) {
				channel.sendMessage("❌ | É necessário ter no mínimo 75% das vagas preenchidas para poder fechar as chaves.").queue();
				return;
			} else if (TournamentDAO.getClosedTournaments().size() > 0) {
				channel.sendMessage("❌ | Já existe um torneio fechado.").queue();
				return;
			}

			channel.sendMessage("Você está prestes a liberar as chaves do torneio `" + t.getName() + "`, deseja confirmar (novas inscrições serão fechadas)?").queue(
					s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
								t.close();
								TournamentDAO.save(t);

								for (Participant p : t.getParticipants()) {
									try {
										Main.getInfo().getUserByID(p.getUid()).openPrivateChannel()
												.flatMap(c -> c.sendMessage("As chaves do torneio `" + t.getName() + "` foram liberadas, fale com o organizador para mais detalhes."))
												.queue(null, MiscHelper::doNothing);
									} catch (Exception ignore) {
									}
								}

								s.delete().queue(null, MiscHelper::doNothing);
								channel.sendMessage("✅ | Chaves liberadas com sucesso, os jogadores foram notificados (use `" + prefix + "torneio ID` para ver as chaves)!").queue();
							}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES
							, u -> u.getId().equals(author.getId())
					), MiscHelper::doNothing
			);
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}