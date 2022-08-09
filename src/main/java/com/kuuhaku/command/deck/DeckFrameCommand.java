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

package com.kuuhaku.command.deck;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.FrameColor;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
		name = "deck",
		subname = "frame",
		category = Category.MISC
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class DeckFrameCommand implements Executable {
	private static final String URL = "https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/master/src/main/resources/shoukan/frames/front/%s.png";

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		Deck d = data.profile().getAccount().getCurrentDeck();
		if (d == null) {
			event.channel().sendMessage(locale.get("error/no_deck", data.config().getPrefix())).queue();
			return;
		}

		if (args.isEmpty()) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor(locale.get("str/all_frames"));

			FrameColor[] frames = FrameColor.values();
			List<Page> pages = new ArrayList<>();
			for (FrameColor fc : frames) {
				if (!fc.canUse(acc)) {
					Title title = fc.getTitle();
					assert title != null;

					eb.setThumbnail("https://indyme.com/wp-content/uploads/2020/11/lock-icon.png")
							.setTitle(locale.get("str/frame_locked"))
							.setDescription(locale.get("str/frame_locked_desc", title.getInfo(locale).getName()));
				} else {
					eb.setImage(URL.formatted(fc.name().toLowerCase(Locale.ROOT)))
							.setTitle(fc.getName(locale))
							.setDescription(fc.getDescription(locale));
				}

				pages.add(new InteractPage(eb.build()));
			}

			AtomicInteger i = new AtomicInteger();
			event.channel().sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.buttonize(s, new LinkedHashMap<>() {{
								put(Utils.parseEmoji("◀️"), w -> {
									if (i.get() > 1) {
										s.editMessageEmbeds((MessageEmbed) pages.get(i.decrementAndGet()).getContent()).queue();
									}
								});
								put(Utils.parseEmoji("▶️"), w -> {
									if (i.get() < frames.length - 1) {
										s.editMessageEmbeds((MessageEmbed) pages.get(i.incrementAndGet()).getContent()).queue();
									}
								});
								put(Utils.parseEmoji("✅"), w -> {
									FrameColor frame = frames[i.get()];
									if (frame.canUse(acc)) {
										event.channel().sendMessage(locale.get("error/frame_locked")).queue();
										return;
									}

									d.setFrame(frame);
									d.save();
								});
							}},
							true, true, 1, TimeUnit.MINUTES, event.user()::equals
					)
			);
		}
	}
}