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
import com.github.ygimenez.model.EmbedCluster;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command(
        name = "deck",
        path = "frame",
        category = Category.MISC
)
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class DeckFrameCommand implements Executable {
    private static final String URL = "https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/rewrite/src/main/resources/shoukan/frames/%s/%s.png";

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

            FrameSkin[] frames = FrameSkin.values();
            List<Page> pages = new ArrayList<>();
            for (int i = 0; i < frames.length; i++) {
                FrameSkin fc = frames[i];
                if (!fc.canUse(acc)) {
                    List<Title> titles = fc.getTitles();
                    String req = Utils.properlyJoin(locale.get("str/and")).apply(
                            titles.stream()
                                    .map(t -> "**`" + t.getInfo(locale).getName() + "`**")
                                    .toList()
                    );

                    eb.setThumbnail("https://i.imgur.com/PXNqRvA.png")
                            .setImage(null)
                            .setTitle(locale.get("str/frame_locked"))
                            .setDescription(locale.get("str/requires_titles", req))
                            .setFooter(locale.get("str/page", i + 1, frames.length));

                    pages.add(InteractPage.of(eb.build()));
                } else {
                    EmbedCluster embeds = new EmbedCluster();

                    eb.setTitle(fc.getName(locale), URL.formatted("front", fc.name().toLowerCase()))
                            .setDescription(fc.getDescription(locale))
                            .setFooter(locale.get("str/page", i + 1, frames.length));

                    embeds.getEmbeds().add(eb.setImage(URL.formatted("front", fc.name().toLowerCase())).build());
                    embeds.getEmbeds().add(eb.setImage(URL.formatted("back", fc.name().toLowerCase())).build());

                    pages.add(InteractPage.of(embeds));
                }
            }

            AtomicInteger i = new AtomicInteger();
            Utils.sendPage(event.channel(), pages.getFirst()).queue(s ->
                    Pages.buttonize(s, Utils.with(new LinkedHashMap<>(), m -> {
                                m.put(Utils.parseEmoji("◀️"), w -> {
                                    if (i.get() > 1) {
                                        s.editMessageEmbeds(Utils.getEmbeds(pages.get(i.decrementAndGet()))).queue();
                                    }
                                });
                                m.put(Utils.parseEmoji("▶️"), w -> {
                                    if (i.get() < frames.length - 1) {
                                        s.editMessageEmbeds(Utils.getEmbeds(pages.get(i.incrementAndGet()))).queue();
                                    }
                                });
                                m.put(Utils.parseEmoji("✅"), w -> {
                                    FrameSkin frame = frames[i.get()];
                                    if (!frame.canUse(acc)) {
                                        event.channel().sendMessage(locale.get("error/frame_locked")).queue();
                                        return;
                                    }

                                    d.getStyling().setFrame(frame);
                                    d.save();
                                    event.channel().sendMessage(locale.get("success/frame_selected", d.getName()))
                                            .flatMap(ms -> s.delete())
                                            .queue();
                                });
                            }),
                            true, true, 1, TimeUnit.MINUTES, event.user()::equals
                    )
            );
        }
    }
}
