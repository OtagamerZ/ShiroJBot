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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.BuffType;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.enums.Tag;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.persistent.VoiceTime;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Command(
		name = "eu",
		aliases = {"meustatus", "mystats"},
		category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
@SlashGroup("meus")
@SlashCommand(name = "stats")
public class MyStatsCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMember(author.getId(), guild.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
		Set<Tag> tags = Tag.getTags(member);

		Map<Emoji, Page> categories = new LinkedHashMap<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		{
			VoiceTime vt = Helper.getOr(ShiroInfo.getShiroEvents().getVoiceTimes().get(mb.getUid() + mb.getSid()), VoiceTimeDAO.getVoiceTime(author.getId(), guild.getId()));
			if (vt.getTime() > 0)
				eb.addField(":timer: | Tempo em canais de voz:", Helper.toStringDuration(vt.getTime()), false);

			StringBuilder badges = new StringBuilder();
			for (Tag t : tags) {
				badges.append(t.getEmote() == null ? "" : t.getEmote().getTag(mb.getLevel()));
			}

			eb.addField("Emblemas:", badges.toString(), false);
			if (mb.getLevel() >= 5)
				eb.setThumbnail(TagIcons.getLevelEmote(mb.getLevel()).getImageUrl());

			categories.put(Helper.parseEmoji("\uD83D\uDD23"), new InteractPage(eb.build()));
		}

		eb.clear();

		{
			boolean waifu = guild.getMembers().stream().map(Member::getId).toList().contains(com.kuuhaku.model.persistent.Member.getWaifu(author.getId()));

			int xp = (int) (15
					* (waifu ? WaifuDAO.getMultiplier(author.getId()).getMult() : 1)
					* Helper.getBuffMult(gc, BuffType.XP)
			);

			float collection = Helper.prcnt(kp.getCards().size(), CardDAO.getTotalCards() * 2);
			if (collection >= 1) xp *= 2;
			else if (collection >= 0.75) xp *= 1.75;
			else if (collection >= 0.5) xp *= 1.5;
			else if (collection >= 0.25) xp *= 1.25;

			String mult = """
					**XP por mensagem:** %s (Base: 15)
					**Chance de spawn de cartas:** %s%% (Base: 3%%)
					**Chance de spawn de drops:** %s%% (Base: 2.5%%)
					**Chance de spawn de cromadas:** %s%% (Base: 0.5%%)
					"""
					.formatted(
							xp,
							Helper.roundToString((3 - Helper.clamp(guild.getMemberCount() / 5000, 0, 1)) * Helper.getBuffMult(gc, BuffType.CARD), 1),
							Helper.roundToString((2.5 - Helper.clamp(guild.getMemberCount() * 0.75f / 5000, 0, 0.75)) * Helper.getBuffMult(gc, BuffType.DROP), 1),
							Helper.roundToString(0.5 * Helper.getBuffMult(gc, BuffType.FOIL), 1)
					);

			eb.addField(":chart_with_upwards_trend: | Seus multiplicadores:", mult, false)
					.setThumbnail(author.getEffectiveAvatarUrl());

			categories.put(Helper.parseEmoji("\uD83D\uDCC8"), new InteractPage(eb.build()));
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

			if (mmr.getRankPoints() == mmr.getTier().getPromRP() || mmr.getTier() == RankedTier.UNRANKED) {
				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < mmr.getPromWins(); i++)
					sb.append(TagIcons.RANKED_WIN.getTag(0).trim());

				for (int i = 0; i < mmr.getPromLosses(); i++)
					sb.append(TagIcons.RANKED_LOSE.getTag(0).trim());

				for (int i = 0; i < mmr.getTier().getMd() - (mmr.getPromWins() + mmr.getPromLosses()); i++)
					sb.append(TagIcons.RANKED_PENDING.getTag(0).trim());

				eb.addField("Progresso para o próximo tier", sb.toString(), false);
			} else
				eb.addField("Progresso para o próximo tier", mmr.getRankPoints() + "/" + mmr.getTier().getPromRP() + " Pontos de Ranking", false);

			if (mmr.getTier().getTier() >= RankedTier.ADEPT_IV.getTier()) {
				eb.addField("Jogos em banca", mmr.getBanked() + "/28", false);
			}

			eb.setThumbnail(ShiroInfo.RESOURCES_URL + "/shoukan/tiers/" + RankedTier.getTierName(mmr.getTier().ordinal(), true).toLowerCase(Locale.ROOT) + ".png");

			categories.put(Helper.parseEmoji("\uD83D\uDCCB"), new InteractPage(eb.build()));
		}

		channel.sendMessageEmbeds((MessageEmbed) categories.get(Helper.parseEmoji("\uD83D\uDD23")).getContent()).queue(s ->
				Pages.categorize(s, categories, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES)
		);
	}
}