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
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.tournament.Participant;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "participantes",
		aliases = {"participants", "parts"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class TournamentPlayersCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário informar o ID do torneio.").queue();
			return;
		}

		try {
			Tournament t = Tournament.find(Tournament.class, Integer.parseInt(args[0]));
			if (t == null) {
				channel.sendMessage("❌ | Torneio inexistente.").queue();
				return;
			} else if (t.getParticipants().isEmpty()) {
				channel.sendMessage("❌ | Ainda não há participantes para esse torneio.").queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Torneio " + t.getName() + " (Chave de " + t.getSize() + ")")
					.setColor(ImageHelper.textToColor(t.getName()))
					.setFooter("Jogadores: " + t.getParticipants().size());

			List<List<Participant>> chunks = CollectionHelper.chunkify(t.getParticipants(), 10);
			List<Page> pages = new ArrayList<>();

			for (List<Participant> chunk : chunks) {
				eb.setDescription(chunk.stream()
						.map(Participant::getUid)
						.map(id -> MiscHelper.getUsername(id) + " **(" + MatchMakingRatingDAO.getMMR(id).getTier() + ")**")
						.collect(Collectors.joining("\n"))
				);

				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(
					s -> Pages.paginate(s, pages, Constants.USE_BUTTONS, 1, TimeUnit.MINUTES)
			);
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID inválido.").queue();
		}
	}
}