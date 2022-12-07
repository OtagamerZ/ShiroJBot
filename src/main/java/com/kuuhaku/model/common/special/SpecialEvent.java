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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class SpecialEvent {
	public static final Map<String, SpecialEvent> EVENTS = new HashMap<>();

	private final double chance;
	private final long timeout;

	public SpecialEvent(double chance, int time, TimeUnit unit) {
		this.chance = chance;
		this.timeout = TimeUnit.MILLISECONDS.convert(time, unit);
	}

	public double getChance() {
		return chance;
	}

	public long getTimeout() {
		return timeout;
	}

	public abstract void onRun(Message msg);

	public abstract void onCompletion(TextChannel channel, I18N locale);

	public abstract void onTimeout(TextChannel channel, I18N locale);

	public abstract boolean isComplete();
}
