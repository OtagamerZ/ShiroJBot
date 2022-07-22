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

package com.kuuhaku.command.store;

import com.github.ygimenez.model.Page;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.Store;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.XStringBuilder;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Command(
		name = "store",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[help]",
		"<params:text>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class StoreCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Store st = new Store(event.user().getId());

		String[] content = args.getString("params").split("\\s+");
		Pair<CommandLine, Options> cli = Utils.getCardCLI(locale, content, true);
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

		int total = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM stashed_card c WHERE c.price > 0");
		List<StashedCard> results = st.getOffers(cli.getFirst().getOptions());
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/search_result", results.size(), total));

		List<Page> pages = Utils.generatePages(eb, results, 10, sc -> {
			switch (sc.getType()) {
				case KAWAIPON -> {
					KawaiponCard kc = sc.getKawaiponCard();

					return new MessageEmbed.Field(
							"ID: `" + sc.getId() + "` | " + sc,
							"%s%s (%s | %s)%s%s".formatted(
									sc.getCard().getRarity().getEmote(),
									locale.get("type/" + sc.getType()),
									locale.get("rarity/" + sc.getCard().getRarity()),
									sc.getCard().getAnime(),
									kc != null && kc.getQuality() > 0
											? ("\n" + locale.get("str/quality", Utils.roundToString(kc.getQuality(), 1)))
											: "",
									"\n" + locale.get("str/offer", sc.getPrice(), "<@" + sc.getKawaipon().getUid() + ">")
							),
							false
					);
				}
				case EVOGEAR -> {
					Evogear ev = DAO.find(Evogear.class, sc.getCard().getId());

					return new MessageEmbed.Field(
							"ID: `" + sc.getId() + "` | " + sc,
							"%s%s (%s | %s)%s".formatted(
									sc.getCard().getRarity().getEmote(),
									locale.get("type/" + sc.getType()),
									locale.get("rarity/" + sc.getCard().getRarity()) + " " + StringUtils.repeat("★", ev.getTier()),
									sc.getCard().getAnime(),
									"\n" + locale.get("str/offer", sc.getPrice(), "<@" + sc.getKawaipon().getUid() + ">")
							),
							false
					);
				}
				case FIELD -> {
					Field fd = DAO.find(Field.class, sc.getCard().getId());

					return new MessageEmbed.Field(
							"ID: `" + sc.getId() + "` | " + sc,
							"%s%s%s (%s | %s)%s".formatted(
									sc.getCard().getRarity().getEmote(),
									locale.get("type/" + sc.getType()),
									locale.get("rarity/" + sc.getCard().getRarity()),
									sc.getCard().getAnime(),
									switch (fd.getType()) {
										case NONE -> "";
										case DAY -> ":sunny: ";
										case NIGHT -> ":crescent_moon: ";
										case DUNGEON -> ":japanese_castle: ";
									},
									"\n" + locale.get("str/offer", sc.getPrice(), "<@" + sc.getKawaipon().getUid() + ">")
							),
							false
					);
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
