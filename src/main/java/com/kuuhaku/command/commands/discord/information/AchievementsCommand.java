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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Achievement;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.Bag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "conquistas",
		aliases = {"achievements"},
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
@SlashGroup("meus")
@SlashCommand(name = "conquistas")
public class AchievementsCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		List<Page> pages = new ArrayList<>();

		List<List<Achievement>> achs = Helper.chunkify(List.of(Achievement.values()), 10);

		Bag<Achievement.Medal> totalMedals = Achievement.getMedalBag();
		Bag<Achievement.Medal> yourMedals = acc.getMedalBag();

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Conquistas Shoukan (" + acc.getAchievements().size() + "/" + Achievement.values().length + ")")
				.setDescription("""
						<:platinum_trophy:901161662294409346> - %s/%s
						<:gold_trophy:901161662265040966> - %s/%s
						<:silver_trophy:901161662365716520> - %s/%s
						<:bronze_trophy:901161662298619934> - %s/%s
						""".formatted(
						yourMedals.getCount(Achievement.Medal.PLATINUM), totalMedals.getCount(Achievement.Medal.PLATINUM),
						yourMedals.getCount(Achievement.Medal.GOLD), totalMedals.getCount(Achievement.Medal.GOLD),
						yourMedals.getCount(Achievement.Medal.SILVER), totalMedals.getCount(Achievement.Medal.SILVER),
						yourMedals.getCount(Achievement.Medal.BRONZE), totalMedals.getCount(Achievement.Medal.BRONZE)
				))
				.setFooter("As conquistas só podem ser desbloqueadas em partidas ranqueadas.");

		for (List<Achievement> chunk : achs) {
			for (Achievement a : chunk) {
				if (a.isHidden() && !acc.getAchievements().contains(a))
					eb.addField(a.toString(acc), "Desbloqueie a conquista para poder vê-la.", false);
				else
					eb.addField(a.toString(acc), a.getDescription(), false);
			}

			pages.add(new InteractPage(eb.build()));
		}

		channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
				Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}
}
