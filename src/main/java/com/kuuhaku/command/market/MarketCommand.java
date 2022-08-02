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

package com.kuuhaku.command.market;

import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.Market;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Command(
		name = "market",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[help]",
		"<params:text>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class MarketCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Market m = new Market(event.user().getId());

		String[] content = args.getString("params").split("\\s+");
		Pair<CommandLine, Options> cli = Utils.getCardCLI(locale, content, true);
		if (args.containsKey("action")) {
			XStringBuilder sb = new XStringBuilder();
			for (Option opt : cli.getSecond().getOptions()) {
				sb.appendNewLine("`-%s --%s` - %s".formatted(
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

		int total = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM stashed_card c WHERE c.price > 0");
		List<StashedCard> results = m.getOffers(cli.getFirst().getOptions());
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/search_result", results.size(), total))
				.setImage(Constants.API_ROOT + "market/offer/" + locale.name() + "?hide=true&v=" + System.currentTimeMillis());

		int sale;
		StashedCard offer = m.getDailyOffer();
		if (offer != null) {
			sale = offer.getId();
		} else {
			sale = -1;
		}

		List<Page> pages = Utils.generatePages(eb, results, 10, 5, sc -> {
			Account seller = sc.getKawaipon().getAccount();

			String emote = sc.getCard().getRarity().getEmote();
			String type = locale.get("type/" + sc.getType());
			String price = "\n" + (sale == sc.getId()
					? locale.get("str/offer_sale", sc.getPrice(), (int) (sc.getPrice() * 0.8), seller.getName() + " (<@" + seller.getUid() + ">)")
					: locale.get("str/offer", sc.getPrice(), seller.getName() + " (<@" + seller.getUid() + ">)")
			);

			switch (sc.getType()) {
				case KAWAIPON -> {
					KawaiponCard kc = sc.getKawaiponCard();

					return new FieldMimic(
							"`ID: " + sc.getId() + "` | " + sc,
							"%s%s (%s | %s)%s%s".formatted(emote, type,
									locale.get("rarity/" + sc.getCard().getRarity()),
									sc.getCard().getAnime(),
									kc != null && kc.getQuality() > 0
											? ("\n" + locale.get("str/quality", Utils.roundToString(kc.getQuality(), 1)))
											: "",
									price
							)
					).toString();
				}
				case EVOGEAR -> {
					Evogear ev = DAO.find(Evogear.class, sc.getCard().getId());

					return new FieldMimic(
							"`ID: " + sc.getId() + "` | " + sc,
							"%s%s (%s | %s)%s".formatted(emote, type,
									locale.get("rarity/" + sc.getCard().getRarity()) + " " + StringUtils.repeat("★", ev.getTier()),
									sc.getCard().getAnime(),
									price
							)
					).toString();
				}
				case FIELD -> {
					Field fd = DAO.find(Field.class, sc.getCard().getId());

					return new FieldMimic(
							"`ID: " + sc.getId() + "` | " + sc,
							"%s%s%s (%s | %s)%s".formatted(emote, type,
									locale.get("rarity/" + sc.getCard().getRarity()),
									sc.getCard().getAnime(),
									switch (fd.getType()) {
										case NONE -> "";
										case DAY -> ":sunny: ";
										case NIGHT -> ":crescent_moon: ";
										case DUNGEON -> ":japanese_castle: ";
									},
									price
							)
					).toString();
				}
			}

			return null;
		});

		if (pages.isEmpty()) {
			event.channel().sendMessage(locale.get("error/no_result")).queue();
			return;
		}

		Utils.paginate(pages, 5, false, event.channel(), event.user());
	}
}
