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
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.SlotSkin;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.DynamicProperty;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
	private static final String URL = "https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/rewrite/src/main/resources/shoukan/side/%s_bottom.png";

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/all_skins"))
				.setFooter(acc.getBalanceFooter(locale));

		SlotSkin[] skins = SlotSkin.values();
		List<Page> pages = new ArrayList<>();
		for (int i = 0; i < skins.length; i++) {
			SlotSkin skin = skins[i];
			if (!skin.canUse(acc)) {
				List<Title> remaining = skin.getTitles().stream()
						.filter(t -> !acc.hasTitle(t.getId()))
						.toList();

				if (!remaining.isEmpty()) {
					String req = Utils.properlyJoin(locale.get("str/and")).apply(
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
							.setImage(URL.formatted(skin.name().toLowerCase()))
							.setTitle(locale.get("str/skin_locked"))
							.setDescription(locale.get("str/price", locale.get("currency/" + skin.getCurrency(), skin.getPrice())));
				}
			} else {
				eb.setThumbnail(null)
						.setImage(URL.formatted(skin.name().toLowerCase()))
						.setTitle(skin.getName(locale))
						.setDescription(skin.getDescription(locale));
			}
			eb.setFooter(locale.get("str/page", i + 1, skins.length));

			pages.add(InteractPage.of(eb.build()));
		}

		AtomicBoolean confirm = new AtomicBoolean();
		AtomicInteger i = new AtomicInteger();
		Utils.sendPage(event.channel(), pages.get(0)).queue(s ->
				Pages.buttonize(s, Utils.with(new LinkedHashMap<>(), m -> {
							m.put(Utils.parseEmoji("◀️"), w -> {
								if (i.get() > 1) {
									confirm.set(false);
									s.editMessageEmbeds(Utils.getEmbeds(pages.get(i.decrementAndGet()))).queue();
								}
							});
							m.put(Utils.parseEmoji("▶️"), w -> {
								if (i.get() < skins.length - 1) {
									confirm.set(false);
									s.editMessageEmbeds(Utils.getEmbeds(pages.get(i.incrementAndGet()))).queue();
								}
							});
							m.put(Utils.parseEmoji("✅"), w -> {
								SlotSkin skin = skins[i.get()];
								if (!skin.canUse(acc)) {
									List<Title> remaining = skin.getTitles().stream()
											.filter(t -> !acc.hasTitle(t.getId()))
											.toList();

									if (!remaining.isEmpty()) {
										event.channel().sendMessage(locale.get("error/skin_locked")).queue();
									} else {
										if (!acc.hasEnough(skin.getPrice(), skin.getCurrency())) {
											event.channel().sendMessage(locale.get("error/insufficient_" + skin.getCurrency())).queue();
											return;
										} else if (!confirm.getAndSet(true)) {
											w.getHook().setEphemeral(true)
													.sendMessage(locale.get("str/press_again"))
													.queue();
											return;
										}

										if (skin.getCurrency() == Currency.CR) {
											acc.consumeCR(skin.getPrice(), "Skin " + skin);
										} else {
											acc.consumeGems(skin.getPrice(), "Skin " + skin);
										}

										DynamicProperty.update(acc.getUid(), "ss_" + skin.name().toLowerCase(), true);
										event.channel().sendMessage(locale.get("success/skin_bought", d.getName()))
												.flatMap(ms -> s.delete())
												.queue();
									}

									return;
								}

								d.getStyling().setSkin(skin);
								d.save();
								event.channel().sendMessage(locale.get("success/skin_selected", d.getName()))
										.flatMap(ms -> s.delete())
										.queue();
							});
						}),
						true, true, 1, TimeUnit.MINUTES, event.user()::equals
				)
		);
	}
}