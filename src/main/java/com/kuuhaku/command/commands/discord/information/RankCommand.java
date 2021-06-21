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
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.RankDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "top10",
		aliases = {"rank", "ranking"},
		usage = "req_rank",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class RankCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			ArrayList<Page> pages = new ArrayList<>();

			int type;
			List<String> data;
			if (args.length > 0 && args[0].equalsIgnoreCase("global")) {
				type = 0;
				data = RankDAO.getLevelRanking(null);
			} else if (Helper.findParam(args, "credit", "creditos", "créditos")) {
				type = 1;
				data = RankDAO.getCreditRanking();
			} else if (Helper.findParam(args, "card", "kawaipon", "cartas")) {
				type = 2;
				data = RankDAO.getCardRanking();
			} else if (Helper.findParam(args, "call", "voice", "voz")) {
				type = 3;
				data = RankDAO.getVoiceRanking(guild.getId());
			} else {
				type = -1;
				data = RankDAO.getLevelRanking(guild.getId());
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Ranking de usuários (" + switch (type) {
						default -> "Level - LOCAL";
						case 0 -> "Level - GLOBAL";
						case 1 -> "Créditos";
						case 2 -> "Cartas";
						case 3 -> "Tempo em call";
					} + ")")
					.setThumbnail("http://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");

			List<List<String>> chunks = Helper.chunkify(data, 15);
			for (int i = 0; i < chunks.size(); i++) {
				eb.clearFields();
				List<String> chunk = chunks.get(i);

				if (i == 0) {
					eb.addField(chunk.get(0), String.join("\n", chunk.subList(1, chunk.size())), false);
				} else {
					eb.addField(Helper.VOID, String.join("\n", chunk), false);
				}

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			m.delete().queue();
			if (pages.isEmpty()) {
				channel.sendMessage("❌ | Não há dados para exibir ainda.").queue();
			} else {
				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
						Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
				);
			}
		});
	}
}
