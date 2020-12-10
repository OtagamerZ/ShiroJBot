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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Tier;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.Tag;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.GuildBuff;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MyStatsCommand extends Command {

	public MyStatsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MyStatsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MyStatsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MyStatsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
		Kawaigotchi kg = KGotchiDAO.getKawaigotchi(author.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		GuildBuff gb = GuildBuffDAO.getBuffs(guild.getId());
		String exceed = ExceedDAO.getExceed(author.getId());
		Set<Tag> tags = Tag.getTags(author, member);

		eb.setTitle(":clipboard: | Status");

		boolean victorious = Main.getInfo().getWinner().equals(ExceedDAO.getExceed(author.getId()));
		boolean waifu = guild.getMembers().stream().map(Member::getId).collect(Collectors.toList()).contains(com.kuuhaku.model.persistent.Member.getWaifu(author));
		boolean kgotchi = kg != null;

		int xp = (int) (15
						* (victorious ? 2 : 1)
						* (waifu ? WaifuDAO.getMultiplier(author).getMult() : 1)
						* (gb.getBuff(1) != null ? gb.getBuff(1).getMult() : 1)
		);

		if (kgotchi) {
			if (kg.isAlive() && kg.getTier() != Tier.CHILD) xp *= kg.getTier().getUserXpMult();
			else if (!kg.isAlive()) xp *= 0.8;
		}

		float collection = Helper.prcnt(kp.getCards().size(), CardDAO.totalCards() * 2);
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
						Helper.round((3 - Helper.minMax(Helper.prcnt(guild.getMemberCount(), 5000), 0, 1)) * (gb.getBuff(2) != null ? gb.getBuff(2).getMult() : 1), 1),
						Helper.round((2.5 - Helper.minMax(Helper.prcnt(guild.getMemberCount() * 0.75f, 5000), 0, 0.75)) * (gb.getBuff(3) != null ? gb.getBuff(3).getMult() : 1), 1),
						Helper.round(0.5 * (gb.getBuff(4) != null ? gb.getBuff(4).getMult() : 1), 1)
				);

		eb.addField(":chart_with_upwards_trend: | Seus multiplicadores:", mult, false);

		StringBuilder badges = new StringBuilder();

		if (!exceed.isEmpty()) {
			badges.append(TagIcons.getExceed(ExceedEnum.getByName(exceed)));
		}

		tags.forEach(t -> badges.append(t.getEmote(mb) == null ? "" : Objects.requireNonNull(t.getEmote(mb)).getTag(mb.getLevel())));

		eb.addField(":label: | Seus emblemas:", badges.toString(), false);

		channel.sendMessage(eb.build()).queue();
	}
}
