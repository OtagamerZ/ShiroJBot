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
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "liga",
		aliases = {"tier", "league"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
@SlashGroup("shoukan")
@SlashCommand(name = "ranking")
public class TierCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();

		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
		if (mmr.getTier() == RankedTier.UNRANKED) {
			channel.sendMessage("❌ | Você ainda não possui ranking no Shoukan, jogue mais partidas ranqueadas.").queue();
			return;
		}

		List<MatchMakingRating> rank = MatchMakingRatingDAO.getMMRRank(mmr.getTier());
		rank.sort(Comparator
				.<MatchMakingRating>comparingInt(m -> m.getTier().ordinal()).reversed()
				.thenComparingInt(m -> m.getPromWins() + m.getPromLosses()).reversed()
				.thenComparing(m -> m.getPromWins() > m.getPromLosses() ? 0 : 1)
				.thenComparingInt(MatchMakingRating::getRankPoints).reversed()
				.thenComparingLong(MatchMakingRating::getMMR).reversed()
		);

		List<List<MatchMakingRating>> tier = Helper.chunkify(rank, 10);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Ranking do tier %s (%s) ".formatted(mmr.getTier().getTier(), mmr.getTier().getName()));

		StringBuilder sb = new StringBuilder();
		StringBuilder prom = new StringBuilder();
		boolean firstPass = true;
		for (List<MatchMakingRating> chunk : tier) {
			sb.setLength(0);

			for (MatchMakingRating mm : chunk) {
				if (mm.getRankPoints() == mmr.getTier().getPromRP()) {
					StringBuilder md = new StringBuilder();

					for (int i = 0; i < mm.getPromWins(); i++)
						md.append(TagIcons.RANKED_WIN.getTag(0).trim());

					for (int i = 0; i < mm.getPromLosses(); i++)
						md.append(TagIcons.RANKED_LOSE.getTag(0).trim());

					for (int i = 0; i < mm.getTier().getMd() - (mm.getPromWins() + mm.getPromLosses()); i++)
						md.append(TagIcons.RANKED_PENDING.getTag(0).trim());

					prom.append("**%s - %s**\n".formatted(mm.getUser().getName(), md.toString()));
				} else
					sb.append("%s - %s PDR\n".formatted(mm.getUser().getName(), mm.getRankPoints()));
			}

			if (firstPass) eb.addField("Promoção de tier", prom.toString(), false);
			eb.addField(Helper.VOID, sb.toString(), false);
			pages.add(new InteractPage(eb.build()));
			firstPass = false;
		}

		channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
