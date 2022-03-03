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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.RankDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LazyLoadingList;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.XStringBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.concurrent.TimeUnit;

@Command(
		name = "top10",
		aliases = {"rank", "ranking"},
		usage = "req_rank",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class RankCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Gerando placares...").queue(m -> {
			int type;
			if (args.length > 0 && args[0].equalsIgnoreCase("global")) {
				type = 0;
			} else if (Helper.findParam(args, "credit", "creditos", "CR")) {
				type = 1;
			} else if (Helper.findParam(args, "card", "kawaipon", "cartas")) {
				type = 2;
			} else if (Helper.findParam(args, "call", "voice", "voz")) {
				type = 3;
			} else {
				type = -1;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle("Ranking de usuários (" + switch (type) {
						default -> "Level - LOCAL";
						case 0 -> "Level - GLOBAL";
						case 1 -> "CR";
						case 2 -> "Cartas";
						case 3 -> "Tempo em call";
					} + ")")
					.setThumbnail("https://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");

			LazyLoadingList<String> d = new LazyLoadingList<>(i -> switch (type) {
				default -> RankDAO.getLevelRanking(guild.getId(), i);
				case 0 -> RankDAO.getLevelRanking(null, i);
				case 1 -> RankDAO.getCreditRanking(i);
				case 2 -> RankDAO.getCardRanking(i);
				case 3 -> RankDAO.getVoiceRanking(guild.getId(), i);
			}, 30);

			if (d.isEmpty()) {
				m.editMessage("❌ | Não há dados para exibir ainda.").queue();
				return;
			}
			fillData(d, 0, eb);

			m.delete().queue();
			channel.sendMessageEmbeds(eb.build()).queue(s ->
					Pages.lazyPaginate(s, i -> {
						d.setIndex(i / 15);

						if (d.isEmpty()) return null;
						fillData(d, i, eb);

						return new InteractPage(eb.build());
					}, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
		});
	}

	private void fillData(LazyLoadingList<String> data, int page, EmbedBuilder eb) {
		eb.clearFields();

		XStringBuilder sb = new XStringBuilder();
		for (int i = 0; i < 15 || data.current() == null; i++) {
			if (data.index() == 0) continue;

			String line = data.current();
			sb.appendNewLine(line);
			data.next();
		}

		if (page == 0) {
			eb.addField(data.get(0), sb.toString(), false);
		} else {
			eb.addField(Helper.VOID, sb.toString(), false);
		}
	}
}
