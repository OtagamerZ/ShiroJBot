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

package com.kuuhaku.command.kawaipon;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "kawaipon",
		category = Category.INFO
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class KawaiponCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = data.profile().getAccount().getKawaipon();

		int total = DAO.queryNative(Integer.class, "SELECT SUM(count) FROM v_card_counter");
		Pair<Integer, Integer> count = kp.countCards();
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/kawaipon_collection", event.user().getName()))
				.setFooter(locale.get("str/owned_cards",
						Calc.prcntToInt(count.getFirst() + count.getSecond(), total * 2),
						Calc.prcntToInt(count.getFirst(), total),
						Calc.prcntToInt(count.getSecond(), total)
				));

		List<Page> pages = new ArrayList<>();
		int max = (int) Math.ceil(total / 50d);
		for (int i = 1; i <= max; i++) {
			eb.setImage("attachment://kawaipon.png");
			pages.add(new InteractPage(eb.build()));
		}

		byte[] b = getPage(locale, kp, 1);
		assert b != null;

		AtomicInteger i = new AtomicInteger();
		event.channel().sendMessageEmbeds((MessageEmbed) pages.get(0).getContent())
				.addFiles(FileUpload.fromData(b, "kawaipon.png"))
				.queue(s ->
						Pages.buttonize(s, Utils.with(new LinkedHashMap<>(), m -> {
									m.put(Utils.parseEmoji("◀️"), w -> {
										if (i.get() > 1) {
											byte[] img = getPage(locale, kp, i.decrementAndGet());
											if (img != null) {
												s.editMessageAttachments(AttachedFile.fromData(img, "kawaipon.png")).queue();
												return;
											}

											i.incrementAndGet();
										}
									});
									m.put(Utils.parseEmoji("▶️"), w -> {
										byte[] img = getPage(locale, kp, i.getAndIncrement());
										if (img != null) {
											s.editMessageAttachments(AttachedFile.fromData(img, "kawaipon.png")).queue();
											return;
										}

										i.decrementAndGet();
									});
								}),
								true, true, 1, TimeUnit.MINUTES, event.user()::equals
						)
				);
	}

	private byte[] getPage(I18N locale, Kawaipon kp, int page) {
		String url = Constants.API_ROOT + "kawaipon/%s/%s?v=%s&page=%s";
		BufferedImage bi = IO.getImage(url.formatted(locale, kp.getUid(), System.currentTimeMillis(), page));
		if (bi == null) return null;

		return IO.getBytes(bi, "png");
	}
}
