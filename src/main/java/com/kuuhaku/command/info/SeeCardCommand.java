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

package com.kuuhaku.command.info;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.FieldType;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

@Command(
		name = "see",
		category = Category.INFO
)
@Signature("<card:word:r> <kind:word>[n,c,s]")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class SeeCardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase());
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card WHERE rarity NOT IN ('ULTIMATE', 'NONE')");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		int stored = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM stashed_card WHERE kawaipon_uid = ?1 AND card_id = ?2",
				event.user().getId(),
				card.getId()
		);
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/in_stash", stored))
				.setTitle(card.getName() + " (" + card.getAnime() + ")")
				.setImage("attachment://card.png");

		if (card.equals(kp.getFavCard())) {
			eb.addField(locale.get("str/favored").toUpperCase(), Constants.VOID, false);
		}

		String type = args.getString("kind", "n").toLowerCase();
		BufferedImage bi = null;
		switch (type) {
			case "n", "c" -> {
				if (card.getRarity().getIndex() == -1) {
					event.channel().sendMessage(locale.get("error/not_kawaipon")).queue();
					return;
				}

				boolean chrome = type.equals("c");
				KawaiponCard kc = kp.getCard(card, chrome);
				if (kc == null) {
					bi = ImageFilters.silhouette(card.drawCard(chrome));
					Graph.overlay(bi, IO.getResourceAsImage("kawaipon/missing.png"));
				} else {
					bi = card.drawCard(chrome);
				}

				if (kc != null) {
					XStringBuilder sb = new XStringBuilder();
					sb.appendNewLine(locale.get("str/quality", Utils.roundToString(kc.getQuality(), 1)));
					sb.appendNewLine(locale.get("str/suggested_price", kc.getSuggestedPrice()));

					eb.addField(locale.get("str/information"), sb.toString(), true);
				}

				Senshi senshi = DAO.find(Senshi.class, card.getId());
				if (senshi != null) {
					if (senshi.isFusion()) {
						eb.addField(locale.get("str/shoukan_enabled"), locale.get("icon/alert") + " " + locale.get("str/as_fusion"), true);
					} else {
						eb.addField(locale.get("str/shoukan_enabled"), locale.get("icon/success") + " " + locale.get("str/yes"), true);
					}
				} else {
					eb.addField(locale.get("str/shoukan_enabled"), locale.get("icon/error") + " " + locale.get("str/no"), true);
				}
			}
			case "s" -> {
				Deck dk = kp.getAccount().getCurrentDeck();
				if (dk == null) {
					event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
					return;
				}

				List<CardType> types = List.copyOf(Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card.getId())));
				if (types.isEmpty()) {
					event.channel().sendMessage(locale.get("error/not_in_shoukan")).queue();
					return;
				}

				Drawable<?> d = switch (types.get(0)) {
					case NONE -> null;
					case KAWAIPON -> DAO.find(Senshi.class, card.getId());
					case EVOGEAR -> DAO.find(Evogear.class, card.getId());
					case FIELD -> DAO.find(Field.class, card.getId());
				};

				if (d != null) {
					bi = d.render(locale, dk);

					if (d instanceof Senshi s && s.isFusion()) {
						eb.setAuthor(null);
					} else if (d instanceof Evogear e && !e.getCharms().isEmpty()) {
						eb.addField(locale.get("str/charms"),
								e.getCharms().stream()
										.map(c -> Charm.valueOf(String.valueOf(c)))
										.map(c -> "**" + c.getName(locale) + ":** " + c.getDescription(locale, e.getTier()))
										.collect(Collectors.joining("\n")),
								false
						);
					} else if (d instanceof Field f && f.getType() != FieldType.NONE) {
						eb.addField(locale.get("field/" + f.getType()), locale.get("field/" + f.getType() + "_desc"), false);
					}

					if (!d.getTags().isEmpty()) {
						eb.addField(locale.get("str/tags"),
								d.getTags().stream()
										.map(s -> {
											if (s.startsWith("race/")) {
												return locale.get(s);
											}

											return d.getString(locale, s);
										})
										.filter(s -> !s.isBlank())
										.map(s -> "`" + s + "`")
										.collect(Collectors.joining(" ")),
								false
						);
					}
				}
			}
		}

		event.channel().sendMessageEmbeds(eb.build())
				.addFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "card.png"))
				.queue();
	}
}
