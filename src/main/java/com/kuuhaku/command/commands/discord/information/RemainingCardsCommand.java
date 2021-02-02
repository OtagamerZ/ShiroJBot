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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "cartasrestantes",
		aliases = {"restante", "remaining"},
		usage = "req_anime",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class RemainingCardsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa especificar um anime para as cartas que faltam (colocar `_` no lugar de espaços).").queue();
			return;
		} else if (Arrays.stream(AnimeName.validValues()).noneMatch(a -> a.name().equals(args[0].toUpperCase()))) {
			channel.sendMessage("❌ | Anime inválido ou ainda não adicionado, você não quis dizer `" + Helper.didYouMean(args[0], Arrays.stream(AnimeName.validValues()).map(AnimeName::name).toArray(String[]::new)) + "`? (colocar `_` no lugar de espaços)").queue();
			return;
		}

		AnimeName anime = AnimeName.valueOf(args[0].toUpperCase());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		List<Page> pages = new ArrayList<>();
		List<Card> collected = kp.getCards().stream()
				.filter(c -> !c.isFoil())
				.map(KawaiponCard::getCard)
				.filter(c -> c.getAnime().equals(anime))
				.collect(Collectors.toList());
		List<Card> cards = CardDAO.getCardsByAnime(anime);
		cards.sort(Comparator
				.comparing(Card::getRarity, Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
				.thenComparing(Card::getName, String.CASE_INSENSITIVE_ORDER)
		);
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		eb.setTitle(":flower_playing_cards: | Cartas coletadas de " + anime.toString());
		eb.addField("Progresso:", collected.size() + " de " + cards.size() + " (" + (Helper.prcntToInt(collected.size(), cards.size())) + "%)", false);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cards.size(); i++) {
			if (collected.contains(cards.get(i)))
				sb.append("||").append(cards.get(i).getRarity().getEmote()).append(" | ").append(cards.get(i).getName()).append("||\n");
			else
				sb.append(cards.get(i).getRarity().getEmote()).append(" | ").append(cards.get(i).getName()).append("\n");
			if (i % 30 == 0 && i > 0 || i == cards.size() - 1) {
				eb.setDescription(sb.toString());
				sb.setLength(0);
				pages.add(new Page(PageType.EMBED, eb.build()));
			}
		}


		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 1, u -> u.getId().equals(author.getId())));
	}
}
