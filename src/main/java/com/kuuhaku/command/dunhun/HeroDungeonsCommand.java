/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Dungeon;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "hero",
		path = "dungeons",
		category = Category.STAFF
)
public class HeroDungeonsCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Deck d = data.profile().getAccount().getDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		Hero h = d.getHero();
		if (h == null) {
			event.channel().sendMessage(locale.get("error/no_hero", data.config().getPrefix())).queue();
			return;
		}

		List<Dungeon> dgs = DAO.queryAll(Dungeon.class, "SELECT d FROM Dungeon d ORDER BY d.areaLevel, d.id");
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setAuthor(locale.get("str/dungeons"))
				.setImage("attachment://image.png");

		List<Page> pages = new ArrayList<>();
		for (Dungeon dg : dgs) {
			eb.clearFields();

			if (!dg.getMonsterPool().isEmpty()) {
				List<String> mobs = DAO.queryAllNative(String.class, "SELECT name FROM monster_info WHERE locale = ?1 AND id IN ?2",
						locale.name(), dg.getMonsterPool()
				);
				eb.addField(locale.get("str/monster_pool"), Utils.properlyJoin(locale.get("str/and")).apply(mobs), true);
			} else {
				eb.addField(locale.get("str/monster_pool"), locale.get("str/unknown"), true);
			}

			eb.setTitle(dg.getInfo(locale).getName() + " (`" + dg.getId() + "`)")
					.setDescription(dg.getInfo(locale).getDescription())
					.addField(locale.get("str/area_level"), String.valueOf(dg.getAreaLevel()), true);

			pages.add(InteractPage.of(eb.build()));
		}

		AtomicInteger i = new AtomicInteger();
		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(1, TimeUnit.MINUTES)
				.setCanInteract(event.user()::equals)
				.addAction(Utils.parseEmoji("◀️"), w -> {
					if (i.get() > 0) {
						int it = i.decrementAndGet();
						MessageEditAction ma = w.getMessage().editMessageEmbeds(Utils.getEmbeds(pages.get(it)));

						File img = IO.getResourceAsFile(Constants.CARDS_ROOT + "../dungeons/" + dgs.get(it).getId() + ".png");
						if (img != null) {
							ma.setFiles(FileUpload.fromData(img));
						}

						ma.queue();
					}
				})
				.addAction(Utils.parseEmoji("▶️"), w -> {
					if (i.get() < pages.size() - 1) {
						int it = i.incrementAndGet();
						MessageEditAction ma = w.getMessage().editMessageEmbeds(Utils.getEmbeds(pages.get(it)));

						File img = IO.getResourceAsFile(Constants.CARDS_ROOT + "../dungeons/" + dgs.get(it).getId() + ".png");
						if (img != null) {
							ma.setFiles(FileUpload.fromData(img));
						}

						ma.queue();
					}
				});

		helper.apply(Utils.sendPage(event.channel(), pages.getFirst())).queue(s -> Pages.buttonize(s, helper));
	}
}
