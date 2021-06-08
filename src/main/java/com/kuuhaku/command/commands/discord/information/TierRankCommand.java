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
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "ranqueada",
		aliases = {"ranked"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class TierRankCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Map<String, Page> categories = new LinkedHashMap<>();

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		StringBuilder sb = new StringBuilder();
		StringBuilder prom = new StringBuilder();

		for (RankedTier rt : RankedTier.values()) {
			if (rt == RankedTier.UNRANKED) continue;
			eb.clearFields();
			sb.setLength(0);
			prom.setLength(0);

			List<MatchMakingRating> top10 = MatchMakingRatingDAO.getMMRRank(rt.getTier());
			top10 = top10.subList(0, Math.min(10, top10.size()));

			eb.setTitle("Top 10 do tier %s (%s)".formatted(rt.getTier(), RankedTier.getTierName(rt.getTier(), false)));

			for (MatchMakingRating mm : top10) {
				User u = mm.getUser();
				if (u == null) continue;
				if (mm.getRankPoints() == mm.getTier().getPromRP()) {
					StringBuilder md = new StringBuilder();

					for (int i = 0; i < mm.getPromWins(); i++)
						md.append(TagIcons.RANKED_WIN.getTag(0).trim());

					for (int i = 0; i < mm.getPromLosses(); i++)
						md.append(TagIcons.RANKED_LOSE.getTag(0).trim());

					for (int i = 0; i < mm.getTier().getMd() - (mm.getPromWins() + mm.getPromLosses()); i++)
						md.append(TagIcons.RANKED_PENDING.getTag(0).trim());

					prom.append("**%s - %s**\n".formatted(u.getName(), md.toString()));
				} else
					sb.append("%s - %s PDR\n".formatted(u.getName(), mm.getRankPoints()));
			}

			eb.addField("Promoção de tier", prom.toString(), false)
					.addField(Helper.VOID, sb.toString(), false)
					.setThumbnail(ShiroInfo.RESOURCES_URL + "/shoukan/tiers/" + RankedTier.getTierName(rt.getTier(), true).toLowerCase(Locale.ROOT) + ".png");
			categories.put(Helper.getNumericEmoji(rt.getTier()), new Page(PageType.EMBED, eb.build()));
		}

		sb.setLength(0);
		eb.clearFields()
				.setTitle("Tiers do Shoukan ranqueado")
				.setThumbnail("http://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");

		for (int i = 1; i < 8; i++) {
			sb.append("%s | %s\n".formatted(Helper.getNumericEmoji(i), RankedTier.getTierName(i, false)));
		}

		eb.setDescription(sb.toString());

		channel.sendMessage(eb.build()).queue(s ->
				Pages.categorize(s, categories, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
