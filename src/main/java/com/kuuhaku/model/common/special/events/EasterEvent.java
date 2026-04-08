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

package com.kuuhaku.model.common.special.events;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ygimenez.listener.EventHandler;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.special.Egg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.PseudoUser;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//@SpecialEvent.Seasonal(months = Calendar.APRIL)
@Requires({Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI})
public class EasterEvent extends SpecialEvent {
	private final Set<Message> msgs = new HashSet<>();

	public EasterEvent(I18N locale) {
		super(locale, "Usa-tan", "usa-tan/1.png");
	}

	@Override
	public void start(GuildMessageChannel channel) {
		PseudoUser pu = getPersona(channel);
		msgs.clear();

		RichCustomEmoji emj = Main.getApp().getShiro().getEmojiById("828634002197970955");
		assert emj != null;

		int eggs = Calc.rng(2, 4);
		ButtonizeHelper helper = new ButtonizeHelper(false)
				.setCancellable(false)
				.setTimeout(3, TimeUnit.MINUTES)
				.addAction(emj, w -> {
					if (!msgs.remove(w.getMessage())) return;

					User u = w.getUser();
					Egg egg = Egg.random();

					WebhookEmbedBuilder eb = new WebhookEmbedBuilder()
							.setColor(Utils.getRandomColor().getRGB())
							.setTitle(new WebhookEmbed.EmbedTitle(
									getLocale().get("str/content"), null
							))
							.setThumbnailUrl(emj.getImageUrl());

					Account acc = DAO.find(Account.class, u.getId());
					acc.setDynValue("easter_eggs", v -> NumberUtils.toInt(v) + 1);
					acc.addCR(egg.cr(), "Easter egg");

					XStringBuilder sb = new XStringBuilder(getLocale().get("currency/cr", egg.cr()));
					for (UserItem i : egg.items().uniqueSet()) {
						int amount = egg.items().getCount(i);
						acc.addItem(i, amount);
						sb.appendNewLine(amount + "x " + i.getName(getLocale()));
					}

					eb.setDescription(sb.toString());
					pu.send(null, getLocale().get("str/easter_event_found", u.getAsTag()), eb.build());

					if (isComplete()) {
						onCompletion(channel);
					}

					EventHandler handler = Pages.getHandler();
					handler.removeEvent(handler.getEventId(w.getMessage()));
				});

		AtomicInteger count = new AtomicInteger();
		channel.getIterableHistory()
				.forEachAsync(m -> {
					if (Calc.chance(5 * count.getAndIncrement() / 25d)) {
						msgs.add(m);
						count.set(0);
					}

					return msgs.size() < eggs;
				})
				.thenAccept(_ -> {
					for (Message msg : msgs) {
						Pages.buttonize(msg, helper);
					}
				})
				.join();

		pu.send(null, getLocale().get("str/easter_event", msgs.size()));

		Utils.awaitMessage(channel, this::onMessage);
		EXEC.schedule(() -> onTimeout(channel), 3, TimeUnit.MINUTES);
	}

	@Override
	public void onCompletion(GuildMessageChannel channel) {
		EXEC.shutdownNow();
		PseudoUser pu = getPersona(channel);
		pu.send(null, getLocale().get("success/easter_complete"));
	}

	@Override
	public void onTimeout(GuildMessageChannel channel) {
		msgs.clear();
		PseudoUser pu = getPersona(channel);
		pu.send(null, getLocale().get("str/easter_fail"));
	}

	@Override
	public boolean isComplete() {
		return msgs.isEmpty();
	}
}
