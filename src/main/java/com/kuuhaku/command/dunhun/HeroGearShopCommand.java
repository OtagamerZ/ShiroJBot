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

package com.kuuhaku.command.dunhun;

import com.github.ygimenez.model.Page;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.GearAffix;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "hero",
		path = {"gear", "buy"},
		category = Category.MISC
)
@Syntax(allowEmpty = true, value = "<id:number:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class HeroGearShopCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		Hero h = acc.getHero(locale);
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		long shopDay = LocalDate.now().toEpochDay();
		Random rng = new Random(shopDay ^ h.getId().hashCode());
		List<Gear> catalogue = Utils.generate(6 * 3, _ -> Gear.getRandom(h, rng));

		JSONObject dt = h.getExtraData();
		if (dt.has("shop")) {
			dt = dt.getJSONObject("shop");
			long recDt = dt.getLong("record_date");

			if (recDt == shopDay) {
				JSONArray bought = dt.getJSONArray("bought");
				for (int i = 0; i < bought.size(); i++) {
					int id = bought.getInt(i, -1);
					if (id > -1) catalogue.set(id, null);
				}
			} else if (dt.has("bought")) {
				dt.remove("bought");
			}
		} else {
			dt = new JSONObject();
		}

		if (!args.has("id")) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/items_available"));

			AtomicInteger i = new AtomicInteger();
			List<Page> pages = Utils.generatePages(eb, catalogue, 6, 3,
					g -> {
						int idx = i.getAndIncrement();

						if (g != null) {
							FieldMimic fm = new FieldMimic(
									"`" + idx + "` - " + g.getName(locale),
									locale.get("str/price", locale.get("currency/cr", g.getPrice()))
							);

							GearAffix imp = g.getImplicit();
							if (imp != null) {
								imp.getDescription(locale).lines()
										.map(l -> "-# " + l)
										.forEach(fm::appendLine);

								if (!g.getAffixes().isEmpty()) {
									fm.appendLine("-# ────────────────");
								}
							}

							for (String l : g.getAffixLines(locale)) {
								fm.appendLine("-# " + l);
							}

							fm.appendLine("`%s%s`".formatted(data.config().getPrefix(), "hero.gear.buy " + idx));
							return fm.toString();
						} else {
							return "-# " + new FieldMimic(
									"`" + idx + "` - *" + locale.get("str/purchased") + "*",
									""
							);
						}
					},
					(p, t) -> eb.setFooter(acc.getBalanceFooter(locale) + "\n" + locale.get("str/page", p + 1, t))
			);

			if (pages.isEmpty()) {
				event.channel().sendMessage(locale.get("error/shop_empty")).queue();
				return;
			}

			Utils.paginate(pages, 1, true, event.channel(), event.user());
			return;
		}

		int idx = args.getInt("id", -1);
		if (!Calc.between(idx, 0, catalogue.size())) {
			event.channel().sendMessage(locale.get("error/invalid_value_range", 0, catalogue.size() - 1)).queue();
			return;
		}

		Gear gear = catalogue.get(idx);
		if (gear == null) {
			event.channel().sendMessage(locale.get("error/item_purchased")).queue();
			return;
		}

		int value = gear.getPrice();
		if (!acc.hasEnough(gear.getPrice(), Currency.CR)) {
			event.channel().sendMessage(locale.get("error/insufficient_cr")).queue();
			return;
		}

		try {
			String price = locale.get("currency/cr", value);

			JSONObject finalDt = dt;
			Utils.confirm(locale.get("question/item_buy", gear.getName(locale), price), event.channel(), w -> {
						if (acc.hasChanged()) {
							event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
							return true;
						}

						gear.setOwner(h);
						gear.save();

						acc.consumeCR(value, "Gear " + gear.getName(locale));

						JSONArray bought = finalDt.getJSONArray("bought");
						bought.add(idx);
						finalDt.put("bought", bought);
						finalDt.put("record_date", shopDay);

						h.getExtraData().put("shop", finalDt);
						h.save();

						event.channel().sendMessage(locale.get("success/item_buy_single", gear.getName(locale))).queue();
						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}
}
