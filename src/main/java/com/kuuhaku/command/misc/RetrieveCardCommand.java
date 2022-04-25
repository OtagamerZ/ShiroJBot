/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.misc;

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.Stash;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.XStringBuilder;
import com.kuuhaku.utils.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

@Command(
		name = "retrieve",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[help]",
		"<card:word>"
})
public class RetrieveCardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Stash stash = DAO.find(Stash.class, event.user().getId());
		if (stash.getCards().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase(Locale.ROOT));
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM Card");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(Locale.ROOT), names);
			if (sug.getSecond() > 75) {
				event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
				return;
			}

			String[] content = event.message().getContentRaw().split("\\s+");
			content = ArrayUtils.remove(content, 0);
			Pair<CommandLine, Options> cli = Utils.getCardCLI(locale, content, false);
			if (args.containsKey("action")) {
				XStringBuilder sb = new XStringBuilder();
				for (Option opt : cli.getSecond().getOptions()) {
					sb.appendNewLine("**-%s --%s** - %s".formatted(
							opt.getOpt(),
							opt.getLongOpt(),
							opt.getDescription()
					));
				}

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor(locale.get("str/search_params"))
						.setDescription(sb.toString());

				event.channel().sendMessageEmbeds(eb.build()).queue();
				return;
			}

			Map<String, String> filters = new LinkedHashMap<>() {{
				put("n", "AND c.card.id LIKE '%'||?2||'%'");
				put("r", "AND CAST(c.card.rarity AS STRING) LIKE '%'||?3||'%'");
				put("a", "AND c.card.anime.id LIKE '%'||?4||'%'");
				put("c", "AND c.foil = TRUE");
				put("k", "AND c.type = '%s'".formatted(CardType.KAWAIPON));
				put("e", "AND c.type = '%s'".formatted(CardType.EVOGEAR));
				put("f", "AND c.type = '%s'".formatted(CardType.FIELD));
			}};

			List<Object> params = new ArrayList<>() {{
				add(event.user().getId());
			}};
			XStringBuilder query = new XStringBuilder("SELECT c FROM StashedCard c WHERE c.stash.uid = ?1");
			for (Map.Entry<String, String> entry : filters.entrySet()) {
				String opt = cli.getFirst().getOptionValue(entry.getKey());

				if (opt != null) {
					query.appendNewLine(entry.getValue());
					params.add(opt.toUpperCase(Locale.ROOT));
				}
			}

			int total = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM stashed_card c WHERE c.stash_uid = ?1", event.user().getId());
			List<StashedCard> results = DAO.queryAll(StashedCard.class, query.toString(), params.toArray());
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/search_result", results.size(), total));

			List<Page> pages = Utils.generatePages(eb, results, 10, c -> {
				switch (c.getType()) {
					case KAWAIPON -> {
						KawaiponCard kc = new KawaiponCard(c.getCard(), c.isFoil(), c.getQuality());
						return new MessageEmbed.Field(
								kc.getName(),
								"%s%s (%s | %s)".formatted(
										c.getCard().getRarity().getEmote(),
										locale.get("type/" + c.getType()),
										locale.get("rarity/" + kc.getCard().getRarity()),
										kc.getCard().getAnime()
								),
								false
						);
					}
					case EVOGEAR -> {
						//TODO
					}
					case FIELD -> {
						//TODO
					}
				}

				return null;
			});

			if (pages.isEmpty()) {
				event.channel().sendMessage(locale.get("error/no_result")).queue();
				return;
			}

			Utils.paginate(pages, 5, false, event.channel(), event.user());
			return;
		}

		Utils.selectOption(locale, event.channel(), stash, card)
				.thenAccept(sc -> {
					Kawaipon kp = stash.getAccount().getKawaipon();

					switch (sc.getType()) {
						case KAWAIPON -> {
							KawaiponCard kc = kp.getCards().stream()
									.filter(c -> c.getCard().equals(card))
									.filter(c -> c.isFoil() == args.getString("kind").equalsIgnoreCase("f"))
									.findFirst().orElse(null);

							if (kc != null) {
								event.channel().sendMessage(locale.get("error/in_collection")).queue();
								return;
							}

							new KawaiponCard(sc.getUUID(), kp, card, sc.isFoil()).save();
						}
						case EVOGEAR -> {
							//TODO
						}
						case FIELD -> {
							//TODO
						}
					}

					sc.delete();
					event.channel().sendMessage(locale.get("success/card_retrieved")).queue();
				})
				.exceptionally(t -> {
					event.channel().sendMessage("error/not_owned").queue();
					return null;
				});
	}
}
