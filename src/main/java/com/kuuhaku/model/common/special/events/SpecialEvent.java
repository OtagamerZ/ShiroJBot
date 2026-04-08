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

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.PseudoUser;
import com.kuuhaku.util.Utils;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.intellij.lang.annotations.MagicConstant;
import org.reflections.Reflections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SpecialEvent {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	protected @interface Seasonal {
		@MagicConstant(valuesFromClass = Calendar.class) int[] months() default {};
		int cooldown() default 3600;
	}

	private static final Map<String, Pair<Long, SpecialEvent>> running = new HashMap<>();
	private static final Reflections refl = new Reflections("com.kuuhaku.model.common.special");
	private static final Map<Integer, Set<Class<?>>> events = new HashMap<>();

	static {
		Set<Class<?>> evts = refl.getTypesAnnotatedWith(Seasonal.class);
		for (Class<?> event : evts) {
			Seasonal season = event.getDeclaredAnnotation(Seasonal.class);
			if (season == null) continue;

			for (int month : season.months()) {
				events.computeIfAbsent(month, _ -> new HashSet<>()).add(event);
			}
		}
	}

	protected final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor();
	private final String personaName;
	private final String personaIcon;
	private final I18N locale;

	public SpecialEvent(I18N locale, String personaName, String personaIcon) {
		this.locale = locale;
		this.personaName = personaName;
		this.personaIcon = personaIcon;
	}

	public PseudoUser getPersona(GuildMessageChannel channel) {
		return new PseudoUser(personaName, Constants.ORIGIN_RESOURCES + "avatar/" + personaIcon, channel);
	}

	public I18N getLocale() {
		return locale;
	}

	public int[] getMonths() {
		Seasonal season = getClass().getDeclaredAnnotation(Seasonal.class);
		if (season == null) return new int[0];

		return season.months();
	}

	public int getCooldown() {
		Seasonal season = getClass().getDeclaredAnnotation(Seasonal.class);
		if (season == null) return 3600;

		return season.cooldown();
	}

	public abstract void start(GuildMessageChannel channel);

	public boolean onMessage(Message msg) {
		return true;
	}

	public abstract void onCompletion(GuildMessageChannel channel);

	public abstract void onTimeout(GuildMessageChannel channel);

	public abstract boolean isComplete();

	public static boolean hasEvent(Guild guild) {
		running.entrySet().removeIf(e -> e.getValue().getFirst() < System.currentTimeMillis());

		return running.containsKey(guild.getId());
	}

	public static void addEvent(Guild guild, SpecialEvent event) {
		running.put(guild.getId(), new Pair<>(
				System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(event.getCooldown()),
				event
		));
	}

	public static SpecialEvent getEvent(I18N locale, GuildMessageChannel channel, int month) {
		Set<Class<?>> evts = events.get(month);
		if (evts == null) return null;

		evts.removeIf(e -> {
			Requires req = e.getDeclaredAnnotation(Requires.class);
			if (req == null) return false;

			return !channel.getGuild().getSelfMember().hasPermission(channel, req.value());
		});

		if (evts.isEmpty()) return null;

		Class<?> chosen = Utils.getRandomEntry(evts);

		try {
			return (SpecialEvent) chosen.getDeclaredConstructor(I18N.class).newInstance(locale);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to start special event {}", chosen.getSimpleName(), e);
			return null;
		}
	}
}
