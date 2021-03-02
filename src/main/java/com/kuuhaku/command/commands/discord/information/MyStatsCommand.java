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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.enums.Tag;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.GuildBuff;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "eu",
		aliases = {"meustatus", "mystats"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class MyStatsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
		GuildBuff gb = GuildBuffDAO.getBuffs(guild.getId());
		String exceed = ExceedDAO.getExceed(author.getId());
		Set<Tag> tags = Tag.getTags(author, member);
		Account acc = AccountDAO.getAccount(author.getId());
		Map<String, Page> categories = new LinkedHashMap<>();

		{
			eb.addField(":timer: | Tempo médio em calls:", DurationFormatUtils.formatDuration(acc.getAvgVoiceTime(), "H 'horas', m 'minutos e' s 'segundos'"), false);

			StringBuilder badges = new StringBuilder();

			if (!exceed.isEmpty()) {
				badges.append(TagIcons.getExceed(ExceedEnum.getByName(exceed)));
			}

			for (Tag t : tags) {
				badges.append(t.getEmote(mb) == null ? "" : Objects.requireNonNull(t.getEmote(mb)).getTag(mb.getLevel()));
			}

			eb.addField("Emblemas:", badges.toString(), false);

			categories.put("\uD83D\uDD23", new Page(PageType.EMBED, eb.build()));
		}

		eb.clear();

		{
			boolean victorious = Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
			boolean waifu = guild.getMembers().stream().map(Member::getId).collect(Collectors.toList()).contains(com.kuuhaku.model.persistent.Member.getWaifu(author.getId()));

			int xp = (int) (15
							* (victorious ? 2 : 1)
							* (waifu ? WaifuDAO.getMultiplier(author).getMult() : 1)
							* (gb.getBuff(1) != null ? gb.getBuff(1).getMult() : 1)
			);

			float collection = Helper.prcnt(kp.getCards().size(), CardDAO.totalCards() * 2);
			if (collection >= 1) xp *= 2;
			else if (collection >= 0.75) xp *= 1.75;
			else if (collection >= 0.5) xp *= 1.5;
			else if (collection >= 0.25) xp *= 1.25;

			String mult = """
					**XP por mensagem:** %s (Base: 15)
					**Taxa de venda:** %s%% (Base: 10%%)
					**Chance de spawn de cartas:** %s%% (Base: 3%%)
					**Chance de spawn de drops:** %s%% (Base: 2.5%%)
					**Chance de spawn de cromadas:** %s%% (Base: 0.5%%)
					"""
					.formatted(
							xp,
							Helper.isTrustedMerchant(author.getId()) ? 5 : 10,
							Helper.round((3 - Helper.clamp(Helper.prcnt(guild.getMemberCount(), 5000), 0, 1)) * (gb.getBuff(2) != null ? gb.getBuff(2).getMult() : 1), 1),
							Helper.round((2.5 - Helper.clamp(Helper.prcnt(guild.getMemberCount() * 0.75f, 5000), 0, 0.75)) * (gb.getBuff(3) != null ? gb.getBuff(3).getMult() : 1), 1),
							Helper.round(0.5 * (gb.getBuff(4) != null ? gb.getBuff(4).getMult() : 1), 1)
					);

			eb.addField(":chart_with_upwards_trend: | Seus multiplicadores:", mult, false);

			categories.put("\uD83D\uDCC8", new Page(PageType.EMBED, eb.build()));
		}

		eb.clear();

		{
			eb.setTitle(":clipboard: | Estatísticas de Shoukan")
					.addField("Estatísticas de jogo", """
							%s vitórias
							%s derrotas
							Taxa de vitórias: %s
							""".formatted(mmr.getWins(), mmr.getLosses(), mmr.getWinrate()
					), false)
					.addField("Ranking no Shoukan", mmr.getTier().getName(), false);

			if (mmr.getRankPoints() == 100 || mmr.getTier() == RankedTier.UNRANKED) {
				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < mmr.getPromWins(); i++)
					sb.append(TagIcons.RANKED_WIN.getTag(0).trim());

				for (int i = 0; i < mmr.getPromLosses(); i++)
					sb.append(TagIcons.RANKED_LOSE.getTag(0).trim());

				for (int i = 0; i < mmr.getTier().getMd() - (mmr.getPromWins() + mmr.getPromLosses()); i++)
					sb.append(TagIcons.RANKED_PENDING.getTag(0).trim());

				eb.addField("Progresso para o próximo tier", sb.toString(), false);
			} else
				eb.addField("Progresso para o próximo tier", mmr.getRankPoints() + "/100 Pontos de Ranking", false);

			categories.put("\uD83D\uDCCB", new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) categories.get("\uD83D\uDD23").getContent()).queue(s ->
				Pages.categorize(s, categories, 1, TimeUnit.MINUTES)
		);
	}
}
