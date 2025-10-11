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

package com.kuuhaku.command.deck;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.DailyDeck;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.SlotSkin;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.DynamicProperty;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "deck",
		path = "skin",
		category = Category.MISC
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class DeckSkinCommand implements Executable {
	private static final String URL = Shoukan.SKIN_PATH + "/%s_bottom.png";

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		} else if (d instanceof DailyDeck) {
			event.channel().sendMessage(locale.get("error/daily_deck")).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/all_skins"));

		List<SlotSkin> skins = DAO.findAll(SlotSkin.class);
		List<Page> pages = new ArrayList<>();
		for (int i = 0; i < skins.size(); i++) {
			SlotSkin skin = skins.get(i);
			if (!skin.canUse(acc)) {
				List<Title> remaining = skin.getTitles().stream()
						.filter(t -> !acc.hasTitle(t))
						.toList();

				if (!remaining.isEmpty()) {
					String req = Utils.properlyJoin(locale,
							remaining.stream()
									.map(t -> "**`" + t.getInfo(locale).getName() + "`**")
									.toList()
					);

					eb.setThumbnail("https://i.imgur.com/PXNqRvA.png")
							.setImage(null)
							.setTitle(locale.get("str/skin_locked"))
							.setDescription(locale.get("str/requires_titles", req));
				} else {
					eb.setThumbnail("https://i.imgur.com/PXNqRvA.png")
							.setImage(URL.formatted(skin.getId().toLowerCase()))
							.setTitle(locale.get("str/skin_locked"))
							.setDescription(locale.get("str/price", locale.get("currency/" + skin.getCurrency(), skin.getPrice())));
				}
			} else {
				eb.setThumbnail(null)
						.setImage(URL.formatted(skin.getId().toLowerCase()))
						.setTitle(skin.getInfo(locale).getName())
						.setDescription(skin.getInfo(locale).getDescription());
			}
			eb.setFooter(acc.getBalanceFooter(locale) + " | " + locale.get("str/page", i + 1, skins.size()));

			pages.add(InteractPage.of(eb.build()));
		}

		AtomicBoolean confirm = new AtomicBoolean();
		AtomicInteger i = new AtomicInteger();
		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(event.user()::equals)
				.addAction(Utils.parseEmoji("⏮️"), w -> {
					if (i.get() > 0) {
						confirm.set(false);
						i.set(0);
						w.getMessage().editMessageEmbeds(Utils.getEmbeds(pages.getFirst())).queue();
					}
				})
				.addAction(Utils.parseEmoji("◀️"), w -> {
					if (i.get() > 0) {
						confirm.set(false);
						w.getMessage().editMessageEmbeds(Utils.getEmbeds(pages.get(i.decrementAndGet()))).queue();
					}
				})
				.addAction(Utils.parseEmoji(Constants.ACCEPT), w -> {
					SlotSkin skin = skins.get(i.get());
					if (!skin.canUse(acc)) {
						List<Title> remaining = skin.getTitles().stream()
								.filter(t -> !acc.hasTitle(t))
								.toList();

						if (!remaining.isEmpty()) {
							event.channel().sendMessage(locale.get("error/skin_locked")).queue();
						} else {
							if (!acc.hasEnough(skin.getPrice(), skin.getCurrency())) {
								event.channel().sendMessage(locale.get("error/insufficient_" + skin.getCurrency())).queue();
								return;
							}

							if (!confirm.getAndSet(true)) {
								Objects.requireNonNull(w.getHook())
										.setEphemeral(true)
										.sendMessage(locale.get("str/press_again"))
										.queue();
								return;
							}

							if (acc.hasChanged()) {
								event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
								return;
							}

							switch (skin.getCurrency()) {
								case CR -> acc.consumeCR(skin.getPrice(), "Skin " + skin);
								case GEM -> acc.consumeGems(skin.getPrice(), "Skin " + skin);
							}

							DynamicProperty.update(acc.getUid(), "ss_" + skin.getId().toLowerCase(), true);
							event.channel().sendMessage(locale.get("success/skin_bought", d.getName()))
									.flatMap(ms -> w.getMessage().delete())
									.queue();
						}

						return;
					}

					d.getStyling().setSkin(skin);
					d.save();

					event.channel().sendMessage(locale.get("success/skin_selected", d.getName()))
							.flatMap(ms -> w.getMessage().delete())
							.queue();
				})
				.addAction(Utils.parseEmoji("▶️"), w -> {
					if (i.get() < skins.size() - 1) {
						confirm.set(false);
						w.getMessage().editMessageEmbeds(Utils.getEmbeds(pages.get(i.incrementAndGet()))).queue();
					}
				})
				.addAction(Utils.parseEmoji("⏭️"), w -> {
					if (i.get() < skins.size() - 1) {
						confirm.set(false);
						i.set(skins.size() - 1);
						w.getMessage().editMessageEmbeds(Utils.getEmbeds(pages.getLast())).queue();
					}
				});

		helper.apply(Utils.sendPage(event.channel(), pages.getFirst())).queue(s -> Pages.buttonize(s, helper));
	}
}
