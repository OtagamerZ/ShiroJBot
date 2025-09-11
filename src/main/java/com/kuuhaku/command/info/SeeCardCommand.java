/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.FieldType;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.*;
import com.ygimenez.json.JSONObject;
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
@Syntax("<card:word:r> <kind:word>[n,c,s]")
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
			String sug = Utils.didYouMean(args.getString("card"), "SELECT id AS value FROM v_card_names");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_card_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			}
			return;
		}

		int stored = DAO.queryNative(Integer.class, """
				SELECT count(1)
				FROM stashed_card
				WHERE kawaipon_uid = ?1
				  AND card_id = ?2
				  AND NOT in_collection
				""", event.user().getId(), card.getId()
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
				StashedCard sc = kp.getCard(card, chrome);
				if (sc == null) {
					bi = card.drawCard(false);
					ImageFilters.silhouette(bi);
					Graph.overlay(bi, IO.getResourceAsImage("kawaipon/missing.png"));
				} else {
					bi = sc.render();
				}

				if (sc != null) {
					XStringBuilder sb = new XStringBuilder();
					sb.appendNewLine(locale.get("str/quality", Utils.roundToString(sc.getQuality(), 1)));
					sb.appendNewLine(locale.get("str/suggested_price", sc.getSuggestedPrice()));

					eb.addField(locale.get("str/information"), sb.toString(), true);
				}

				Senshi senshi = card.asSenshi();
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
				Deck dk = kp.getAccount().getDeck();
				if (dk == null) {
					event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
					return;
				}

				List<CardType> types = List.copyOf(Bit32.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card.getId())));
				if (types.isEmpty()) {
					event.channel().sendMessage(locale.get("error/not_in_shoukan")).queue();
					return;
				}

				Drawable<?> d = switch (types.getLast()) {
					case SENSHI -> card.asSenshi();
					case EVOGEAR -> card.asEvogear();
					case FIELD -> card.asField();
					default -> null;
				};

				if (d == null) {
					event.channel().sendMessage(locale.get("error/not_in_shoukan")).queue();
					return;
				}

				bi = d.render(locale, dk);
				eb.setDescription(((EffectHolder<?>) d).getReadableDescription(locale));

				switch (d) {
					case Senshi s -> {
						if (s.getCard().getRarity() == Rarity.NONE) {
							if (!eb.getDescriptionBuilder().isEmpty()) {
								eb.appendDescription("\n\n");
							}

							eb.appendDescription("**" + locale.get("str/effect_only") + "**");
						}

						if (s.isFusion()) {
							eb.setAuthor(null);
						}
					}
					case Evogear e when !e.getCharms().isEmpty() -> {
						if (e.getTier() < 0) {
							if (!eb.getDescriptionBuilder().isEmpty()) {
								eb.appendDescription("\n\n");
							}

							eb.appendDescription("**" + locale.get("str/effect_only") + "**");
						}

						eb.addField(locale.get("str/charms"),
								e.getCharms().stream()
										.map(c -> Charm.valueOf(String.valueOf(c)))
										.map(c -> "**" + c.getName(locale) + ":** " + c.getDescription(locale, e.getTier()))
										.collect(Collectors.joining("\n")),
								false
						);
					}
					case Field f -> {
						if (f.isEffectOnly()) {
							if (!eb.getDescriptionBuilder().isEmpty()) {
								eb.appendDescription("\n\n");
							}

							eb.appendDescription("**" + locale.get("str/effect_only") + "**");
						}

						if (f.getType() != FieldType.NONE) {
							eb.addField(locale.get("field/" + f.getType()), locale.get("field/" + f.getType() + "_desc"), false);
						}
					}
					default -> {
					}
				}

				if (!d.getTagBundle().isEmpty()) {
					eb.addField(locale.get("str/tags"),
							d.getTags(locale).stream()
									.map(s -> "`" + s + "`")
									.collect(Collectors.joining(" ")),
							false
					);
				}
			}
		}

		event.channel().sendMessageEmbeds(eb.build())
				.addFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "card.png"))
				.queue();
	}
}
