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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TierCommand extends Command {

	public TierCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public TierCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public TierCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public TierCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();

		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
		if (mmr.getTier() == RankedTier.UNRANKED) {
			channel.sendMessage("❌ | Você ainda não possui ranking no Shoukan, jogue mais partidas ranqueadas.").queue();
			return;
		}

		List<List<MatchMakingRating>> lobby = Helper.chunkify(MatchMakingRatingDAO.getMMRRank(mmr.getTier()), 10);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Ranking do tier %s (%s) ".formatted(mmr.getTier().getTier(), mmr.getTier().getName()));

		StringBuilder sb = new StringBuilder();
		StringBuilder prom = new StringBuilder();
		for (List<MatchMakingRating> chunk : lobby) {
			sb.setLength(0);

			for (MatchMakingRating mm : chunk) {
				if (mm.getRankPoints() == 100) {
					StringBuilder md = new StringBuilder();

					for (int i = 0; i < mm.getPromWins(); i++)
						md.append(TagIcons.RANKED_WIN.getTag(0).trim());

					for (int i = 0; i < mm.getPromLosses(); i++)
						md.append(TagIcons.RANKED_LOSE.getTag(0).trim());

					for (int i = 0; i < mm.getTier().getMd() - (mm.getPromWins() + mm.getPromLosses()); i++)
						md.append(TagIcons.RANKED_PENDING.getTag(0).trim());

					prom.append("%s - %s\n".formatted(mm.getUser().getName(), md.toString()));
				} else
					sb.append("%s - %s PDR\n".formatted(mm.getUser().getName(), mm.getRankPoints()));
			}

			eb.addField("Promoção de tier", prom.toString(), false)
					.addField(Helper.VOID, sb.toString(), false);
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
