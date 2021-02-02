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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.RankedQueue;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "saguao",
		aliases = {"lobby"},
		usage = "req_queue-exit",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class LobbyCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length > 0 && Helper.equalsAny(args[0], "sair", "exit")) {
			MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
			Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr);
			Main.getInfo().getMatchMaking().getDuoLobby().remove(mmr);
			channel.sendMessage("Você saiu do saguão com sucesso.").queue();
			return;
		} else if (args.length < 1 || !Helper.equalsAny(args[0], "solo", "duo")) {
			channel.sendMessage("❌ | Você precisa informar o tipo de fila que deseja entrar (`SOLO` ou `DUO`)").queue();
			return;
		}

		RankedQueue rq = RankedQueue.valueOf(args[0].toUpperCase());
		List<Page> pages = new ArrayList<>();

		List<List<MatchMakingRating>> lobby = switch (rq) {
			case SOLO -> Helper.chunkify(Main.getInfo().getMatchMaking().getSoloLobby().keySet(), 10);
			case DUO -> Helper.chunkify(Main.getInfo().getMatchMaking().getDuoLobby().keySet(), 10);
		};

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Saguão do Shoukan ranqueado (%s | %s jogadores)".formatted(rq.name(), switch (rq) {
					case SOLO -> Main.getInfo().getMatchMaking().getSoloLobby().size();
					case DUO -> Main.getInfo().getMatchMaking().getDuoLobby().size();
				}));

		StringBuilder sb = new StringBuilder();
		for (List<MatchMakingRating> chunk : lobby) {
			sb.setLength(0);
			for (MatchMakingRating mmr : chunk)
				sb.append("%s (%s)\n".formatted(mmr.getUser().getName(), mmr.getTier().getName()));

			eb.setDescription(sb.toString());
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
