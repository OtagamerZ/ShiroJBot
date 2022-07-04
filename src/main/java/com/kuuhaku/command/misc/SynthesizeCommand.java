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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Command(
		name = "synth",
		category = Category.MISC
)
@Signature("<card:word:r> <card:word:r> <card:word:r>")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class SynthesizeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<StashedCard> cards = new ArrayList<>();
		CompletableFuture<Void> setup = new CompletableFuture<>();
		for (Object entry : args.getJSONArray("card")) {
			if (entry instanceof String card) {
				Card c = DAO.find(Card.class, card.toUpperCase(Locale.ROOT));
				if (c == null) {
					List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card");

					Pair<String, Double> sug = Utils.didYouMean(card.toUpperCase(Locale.ROOT), names);
					event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
					return;
				}

				CompletableFuture<Void> select = new CompletableFuture<>();
				List<StashedCard> stash = DAO.queryAll(StashedCard.class,
						"SELECT s FROM StashedCard s WHERE s.kawaipon.uid = ?1 AND s.deck.id IS NULL",
						event.user().getId()
				);
				stash.removeIf(cards::contains);
				Utils.selectOption(locale, event.channel(), stash, c, event.user())
						.thenAccept(sc -> {
							if (sc == null) {
								event.channel().sendMessage(locale.get("error/invalid_value")).queue();
								return;
							} else if (cards.contains(sc)) {
								event.channel().sendMessage(locale.get("error/twice_added")).queue();
								return;
							}

							cards.add(sc);
							select.complete(null);
						})
						.exceptionally(t -> {
							event.channel().sendMessage(locale.get("error/not_owned")).queue();
							select.complete(null);
							return null;
						});

				try {
					select.get();
					setup.complete(null);
				} catch (InterruptedException | ExecutionException ignore) {
				}
			}
		}

		try {
			setup.get();
		} catch (InterruptedException | ExecutionException ignore) {
		}

		if (cards.size() != 3) {
			event.channel().sendMessage(locale.get("error/invalid_synth_material")).queue();
			return;
		}

		Utils.confirm(locale.get("question/synth"), event.channel(), wrapper -> {
					Kawaipon kp = data.profile().getAccount().getKawaipon();
					double field = cards.stream()
							.mapToDouble(sc -> {
								if (sc.getKawaiponCard() != null && sc.getKawaiponCard().isFoil()) {
									return 100 / 3d;
								}

								return 0;
							}).sum();

					if (Calc.chance(field)) {
						Field f = Utils.getRandomEntry(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE f.effect = FALSE"));
						event.channel().sendMessage(locale.get("success/synth", f)).queue();
						new StashedCard(kp, f.getCard(), CardType.FIELD).save();
					} else {
						double inc = 1;
						double more = 1;

						for (StashedCard sc : cards) {
							switch (sc.getType()) {
								case KAWAIPON -> {
									KawaiponCard kc = sc.getKawaiponCard();
									int rarity = kc.getCard().getRarity().getIndex();

									if (kc.isFoil()) {
										more *= 1 + rarity * kc.getQuality() / 200;
									} else {
										inc += rarity * kc.getQuality() / 200;
									}
								}
								case EVOGEAR -> {
									Evogear ev = DAO.find(Evogear.class, sc.getCard().getId());
									inc += ev.getTier() / 4d;
								}
								case FIELD -> more *= 1.1;
							}
						}

						double mult = 1 * inc * more;
						RandomList<Evogear> pool = new RandomList<>((v, f) -> 1 - Math.pow(v, f), 1 * mult);
						List<Evogear> evos = DAO.findAll(Evogear.class);
						for (Evogear evo : evos) {
							if (evo.getTier() <= 0) continue;

							pool.add(evo, 5 - evo.getTier());
						}

						Evogear e = pool.get();
						new StashedCard(kp, e.getCard(), CardType.EVOGEAR).save();
						event.channel().sendMessage(locale.get("success/synth", e + " (" + StringUtils.repeat("★", e.getTier()) + ")")).queue();
					}

					for (StashedCard card : cards) {
						card.delete();
					}
				}, event.user()
		);
	}
}