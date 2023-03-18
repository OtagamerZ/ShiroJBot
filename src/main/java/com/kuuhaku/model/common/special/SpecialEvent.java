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

package com.kuuhaku.model.common.special;

import com.kuuhaku.model.enums.I18N;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SpecialEvent {
	private static final Map<String, Pair<Long, SpecialEvent>> events = new HashMap<>();

	protected final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor();
	private final I18N locale;

	public SpecialEvent(I18N locale) {
		this.locale = locale;
	}

	public I18N getLocale() {
		return locale;
	}

	public abstract void start(GuildMessageChannel channel);

	public abstract boolean onRun(Message msg);

	public abstract void onCompletion(GuildMessageChannel channel);

	public abstract void onTimeout(GuildMessageChannel channel);

	public abstract boolean isComplete();

	public static boolean hasEvent(Guild guild) {
		events.entrySet().removeIf(e -> e.getValue().getFirst() < System.currentTimeMillis());

		return events.containsKey(guild.getId());
	}

	public static void addEvent(Guild guild, SpecialEvent event) {
		events.put(guild.getId(), new Pair<>(
				System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS),
				event
		));
	}
}
