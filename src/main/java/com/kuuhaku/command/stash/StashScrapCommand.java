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
import com.kuuhaku.interfaces.annotations.Signature;
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
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Command(
		name = "stash",
		path = "scrap",
		category = Category.MISC
)
@Signature({
		"<action:word:r>[trash]",
		"<cards:text:r>"
})
public class StashScrapCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());
		if (kp.getStash().isEmpty()) {
			event.channel().sendMessage(locale.get("error/empty_stash")).queue();
			return;
		}

		if (args.has("action")) {
			confirm(locale, kp.getTrash(), event, kp.getAccount());
			return;
		}

		List<StashedCard> cards = new ArrayList<>();
		List<StashedCard> stash = data.profile().getAccount().getKawaipon().getNotInUse();

		for (String id : args.getString("cards").split(" +")) {
			Card card = verifyCard(locale, event, id.toUpperCase());
			if (card == null) return;

			CompletableFuture<Boolean> success = new CompletableFuture<>();
			Utils.selectOption(args.has("confirm"), locale, event.channel(), stash, card, event.user())
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

			try {
				if (!success.get()) return;
			} catch (InterruptedException | ExecutionException ignore) {
			}
		}

		confirm(locale, cards, event, kp.getAccount());
	}

	private Card verifyCard(I18N locale, MessageData.Guild event, String id) {
		Card card = DAO.find(Card.class, id);
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM v_card_names");

			Pair<String, Double> sug = Utils.didYouMean(id, names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
		}

		return card;
	}

	private void confirm(I18N locale, List<StashedCard> cards, MessageData.Guild event, Account acc) {
		try {
			int value = getValue(cards);

			Utils.confirm(locale.get(cards.size() == 1 ? "question/scrap" : "question/scraps", value, cards.size()), event.channel(), w -> {
						event.channel().sendMessage(locale.get("success/scrap")).queue();
						acc.addCR(value, cards.stream().map(StashedCard::toString).collect(Collectors.joining()) + " scrapped");

						Bag<UserItem> items = new HashBag<>();
						for (StashedCard sc : cards) {
							if (sc.getType() == CardType.KAWAIPON) {
								KawaiponCard kc = sc.getKawaiponCard();
								if (kc != null) {
									kc.delete();

									Rarity rarity = kc.getCard().getRarity();
									if (Calc.chance(Math.pow(2.15, 7 - rarity.getIndex()))) {
										UserItem item = DAO.find(UserItem.class, rarity.name() + "_SHARD");
										if (item != null) {
											int amount = Calc.rng(1, 3);
											items.add(item, amount);
										}
									}

									if (kc.isChrome() && Calc.chance(33)) {
										UserItem item = DAO.find(UserItem.class, "CHROMATIC_ESSENCE");
										if (item != null) {
											int amount = Calc.rng(1, 3);
											acc.addItem(item, amount);
											items.add(item, amount);
										}
									}

									continue;
								}
							}

							sc.delete();
						}

						if (!items.isEmpty()) {
							AtomicInteger dist = new AtomicInteger();
							XStringBuilder sb = new XStringBuilder();
							items.stream().distinct()
									.sorted(Comparator.comparing(items::getCount, Comparator.reverseOrder()))
									.forEach(i -> {
										dist.getAndIncrement();

										int amount = items.getCount(i);
										sb.appendNewLine("- " + amount + "x " + i.getInfo(locale));
										acc.addItem(i, amount);
									});

							if (dist.get() == 1) {
								UserItem item = items.stream().findAny().orElseThrow();
								event.channel().sendMessage(locale.get("str/received_item", items.getCount(item), item.getInfo(locale))).queue();
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
				value += (int) (kc.getSuggestedPrice() / 3 * mult);
			} else {
				if (sc.getType() == CardType.EVOGEAR) {
					Evogear e = sc.getCard().asEvogear();
					value += (int) (e.getTier() * 225 * mult);
				} else {
					value += (int) (2500 * mult);
				}
			}
		}

		return value;
	}
}
