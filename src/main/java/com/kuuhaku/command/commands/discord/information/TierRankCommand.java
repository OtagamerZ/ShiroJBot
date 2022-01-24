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

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "ranqueada",
		aliases = {"ranked"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
@SlashGroup("shoukan")
@SlashCommand(name = "top10")
public class TierRankCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Map<Emoji, Page> categories = new LinkedHashMap<>();

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		StringBuilder sb = new StringBuilder();
		StringBuilder prom = new StringBuilder();

		for (RankedTier rt : RankedTier.values()) {
			if (rt == RankedTier.UNRANKED) continue;
			eb.clearFields();
			sb.setLength(0);
			prom.setLength(0);

			List<MatchMakingRating> top10 = MatchMakingRatingDAO.getMMRRank(rt.getTier());
			top10.sort(Comparator
					.<MatchMakingRating>comparingInt(mmr -> mmr.getTier().ordinal()).reversed()
					.thenComparingInt(mmr -> mmr.getPromWins() + mmr.getPromLosses()).reversed()
					.thenComparing(mmr -> mmr.getPromWins() > mmr.getPromLosses() ? 0 : 1)
					.thenComparingInt(MatchMakingRating::getRankPoints).reversed()
					.thenComparingLong(MatchMakingRating::getMMR).reversed()
			);

			top10 = top10.subList(0, Math.min(10, top10.size()));

			eb.setTitle("Top 10 do tier %s (%s)".formatted(rt.getTier(), RankedTier.getTierName(rt.getTier(), false)));

			for (MatchMakingRating mm : top10) {
				User u = mm.getUser();
				if (u == null) continue;

				String tier = mm.getTier().getTier() < 5 ? "(" + mm.getTier().getName().split(" ")[1] + ")" : "";

				if (mm.getRankPoints() == mm.getTier().getPromRP()) {
					StringBuilder md = new StringBuilder();

					for (int i = 0; i < mm.getPromWins(); i++)
						md.append(TagIcons.RANKED_WIN.getTag(0).trim());

					for (int i = 0; i < mm.getPromLosses(); i++)
						md.append(TagIcons.RANKED_LOSE.getTag(0).trim());

					for (int i = 0; i < mm.getTier().getMd() - (mm.getPromWins() + mm.getPromLosses()); i++)
						md.append(TagIcons.RANKED_PENDING.getTag(0).trim());

					prom.append("**%s - %s**%s\n".formatted(u.getName(), md.toString(), tier));
				} else
					sb.append("%s - %s PDR %s\n".formatted(u.getName(), mm.getRankPoints(), tier));
			}

			eb.addField("Promoção de tier", prom.toString(), false)
					.addField(Helper.VOID, sb.toString(), false)
					.setThumbnail(ShiroInfo.RESOURCES_URL + "/shoukan/tiers/" + RankedTier.getTierName(rt.getTier(), true).toLowerCase(Locale.ROOT) + ".png");
			categories.put(Helper.parseEmoji(Helper.getNumericEmoji(rt.getTier())), new InteractPage(eb.build()));
		}

		sb.setLength(0);
		eb.clearFields()
				.setTitle("Tiers do Shoukan ranqueado")
				.setThumbnail("https://www.marquishoa.com/wp-content/uploads/2018/01/Ranking-icon.png");

		for (int i = 1; i < 8; i++) {
			sb.append("%s | %s\n".formatted(Helper.getNumericEmoji(i), RankedTier.getTierName(i, false)));
		}

		eb.setDescription(sb.toString());

		channel.sendMessageEmbeds(eb.build()).queue(s ->
				Pages.categorize(s, categories, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
