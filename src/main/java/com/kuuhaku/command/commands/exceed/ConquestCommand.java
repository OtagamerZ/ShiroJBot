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

package com.kuuhaku.command.commands.exceed;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.enums.Country;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ConquestCommand extends Command {

	public ConquestCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ConquestCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ConquestCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ConquestCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		ExceedEnums ex = ExceedEnums.getByName(ExceedDAO.getExceed(author.getId()));
		PoliticalState state = PStateDAO.getPoliticalState(ex);

		if (!ExceedDAO.hasExceed(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-map-no-exceed")).queue();
			return;
		} else if (!ExceedDAO.getLeader(ex).equals(author.getId())) {
			channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-not-leader"), ex.getName())).queue();
			return;
		} else if (state.getCountries().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-defeated")).queue();
			return;
		} else if (args.length == 0) {
			Map<ExceedEnums, List<Country>> dominions = new HashMap<>();

			PStateDAO.getAllPoliticalState().stream()
					.map(ps -> Map.of(ps.getExceed(), ps.getCountries().stream()
							.map(c -> Country.valueOf(String.valueOf(c)))
							.collect(Collectors.toList())
					))
					.forEach(dominions::putAll);

			Map<String, Page> pages = new LinkedHashMap<>();
			EmbedBuilder eb = new EmbedBuilder();
			dominions.forEach((k, v) -> {
				Emote e = Main.getInfo().getAPI().getEmoteById(TagIcons.getExceedId(k));
				eb.clear();
				eb.setTitle("Domínios de " + k.getName() + " (pontos de influência: " + PStateDAO.getPoliticalState(k).getInfluence() + ")");
				try {
					assert e != null;
					eb.setColor(Helper.colorThief(e.getImageUrl()));
				} catch (IOException | NullPointerException exc) {
					eb.setColor(Helper.getRandomColor());
				}
				eb.setThumbnail(e.getImageUrl());
				String countries = v.stream().map(Country::getName).collect(Collectors.joining("\n"));
				eb.addField("Países:", countries, false);

				pages.put(TagIcons.getExceedId(k), new Page(PageType.EMBED, eb.build()));
			});

			channel.sendMessage("Informe um país para desafiar o líder do Exceed em questão").embed((MessageEmbed) pages.get(TagIcons.getExceedId(ex)).getContent()).queue(s ->
					Pages.categorize(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		if (state.getInfluence() < 100) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-not-enough-influence")).queue();
			return;
		}

		String country = String.join("_", args).toUpperCase();

		try {
			Country.valueOf(country);
		} catch (IllegalArgumentException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-invalid-country")).queue();
			return;
		}

		PoliticalState enemy = PStateDAO.getAllPoliticalState().stream().filter(ps -> ps.getCountries().contains(country)).collect(Collectors.toList()).get(0);

		if (enemy.getExceed() == ex) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-already-owns-country")).queue();
			return;
		}

		Member enemyLeader = Main.getInfo().getMemberByID(ExceedDAO.getLeader(enemy.getExceed()));

		if (enemyLeader.getOnlineStatus() != OnlineStatus.ONLINE || guild.getMemberById(enemyLeader.getId()) == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_exceed-leader-not-ready")).queue();
			return;
		}

		/*channel.sendMessage(enemyLeader.getAsMention() + ", como líder atual da " + enemy.getExceed().getName() + " você foi desafiado a um desafio de sorte.\n" +
				"Quanto mais pontos de influência maior a chance de ganhar. Se deseja aceitar digite o nome de ").queue();
		Helper.awaitMessage(enemyLeader.getUser(), channel, () -> {

		});*/
	}
}
