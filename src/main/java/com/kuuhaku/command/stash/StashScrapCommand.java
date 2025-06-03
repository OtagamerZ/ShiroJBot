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

package com.kuuhaku.command.stash;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.user.*;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.NoResultException;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Command(
		name = "stash",
		path = "scrap",
		category = Category.MISC
)
@Syntax({
		"<action:word:r>[all,extra]",
		"<cards:text:r>"
})
public class StashScrapCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStashUsage() == 0) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		if (args.has("action")) {
			if (args.getString("action").equalsIgnoreCase("all")) {
				confirm(locale, kp.getNotInUse(), event, kp.getAccount());
			} else {
				confirm(locale, kp.getExtras(), event, kp.getAccount());
			}

			return;
		}

		List<StashedCard> cards = new ArrayList<>();
		List<StashedCard> stash = data.profile().getAccount().getKawaipon().getNotInUse();

		String[] ids = args.getString("cards").split(" +");
		if (ids.length > 10) {
			event.channel().sendMessage(locale.get("error/too_many_items", 10)).queue();
			return;
		}

		for (String id : ids) {
			Card card = verifyCard(locale, event, id.toUpperCase());
			if (card == null) return;

			CompletableFuture<Boolean> success = new CompletableFuture<>();
			Utils.selectOption(locale, event.channel(), stash, card, event.user())
					.thenAccept(sc -> {
						if (sc == null) {
							event.channel().sendMessage(locale.get("error/invalid_value")).queue();
							success.complete(false);
							return;
						}

						cards.add(sc);
						stash.remove(sc);
						success.complete(true);
					})
					.exceptionally(t -> {
						if (!(t.getCause() instanceof NoResultException)) {
							Constants.LOGGER.error(t, t);
						}

						event.channel().sendMessage(locale.get("error/not_owned")).queue();
						success.complete(false);
						return null;
					});

			if (!success.join()) return;
		}

		confirm(locale, cards, event, kp.getAccount());
	}

	private Card verifyCard(I18N locale, MessageData.Guild event, String id) {
		Card card = DAO.find(Card.class, id);
		if (card == null) {
			String sug = Utils.didYouMean(id, "SELECT id AS value FROM v_card_names");
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_card_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
			}
		}

		return card;
	}

	private void confirm(I18N locale, List<StashedCard> cards, MessageData.Guild event, Account acc) {
		try {
			int value = getValue(cards);

			Utils.confirm(locale.get(cards.size() == 1 ? "question/scrap" : "question/scraps", value, cards.size()), event.channel(), w -> {
						event.channel().sendMessage(locale.get("success/scrap")).queue();
						acc.addCR(value, "Scrapped " + cards.stream().map(StashedCard::toString).collect(Collectors.joining(", ")));

						List<String> batchKC = new ArrayList<>();
						List<String> batchSC = new ArrayList<>();
						Bag<String> items = new HashBag<>();
						for (StashedCard sc : cards) {
							if (sc.isChrome() && Calc.chance(50)) {
								items.add("CHROMATIC_ESSENCE", Calc.rng(3, 5));
							}

							if (sc.getType() == CardType.KAWAIPON) {
								KawaiponCard kc = sc.getKawaiponCard();
								if (kc != null) {
									batchKC.add(kc.getUUID());

									Rarity rarity = kc.getCard().getRarity();
									if (Calc.chance(Math.pow(2.15, 7 - rarity.getIndex()))) {
										items.add(rarity.name() + "_SHARD", 1);
									}
									continue;
								}
							}

							batchSC.add(sc.getUUID());
						}
						DAO.applyNative(null, "DELETE FROM kawaipon_card WHERE uuid IN ?1", batchKC);
						DAO.applyNative(null, "DELETE FROM stashed_card WHERE uuid IN ?1", batchSC);

						if (!items.isEmpty()) {
							AtomicInteger dist = new AtomicInteger();
							XStringBuilder sb = new XStringBuilder();
							items.stream().distinct()
									.sorted(Comparator.comparing(items::getCount, Comparator.reverseOrder()))
									.forEach(i -> {
										dist.getAndIncrement();

										int amount = items.getCount(i);
										UserItem item = DAO.find(UserItem.class, i);
										if (item != null) {
											sb.appendNewLine("- " + amount + "x " + item.getName(locale));
											acc.addItem(item, amount);
										}
									});

							if (dist.get() == 1) {
								String i = items.stream().findFirst().orElseThrow();
								UserItem item = DAO.find(UserItem.class, i);

								event.channel().sendMessage(locale.get("str/received_item", items.getCount(i), item.getName(locale))).queue();
							} else {
								event.channel().sendMessage(locale.get("str/received_items", sb.toString())).queue();
							}
						}
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}

	private int getValue(StashedCard card) {
		return getValue(List.of(card));
	}

	private int getValue(Collection<StashedCard> cards) {
		int value = 0;
		for (StashedCard sc : cards) {
			double mult = Calc.rng(0.5, 0.8, sc.getId());
			if (sc.getType() == CardType.KAWAIPON) {
				KawaiponCard kc = sc.getKawaiponCard();
				if (kc != null) {
					value += (int) (kc.getSuggestedPrice() / 3.0 * mult);
				}
			} else {
				if (sc.getType() == CardType.EVOGEAR) {
					Evogear e = sc.getCard().asEvogear();
					if (e != null) {
						value += (int) (e.getTier() * 225 * mult);
					}
				} else {
					value += (int) (2500 * mult);
				}
			}
		}

		return value;
	}
}
