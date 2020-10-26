/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.drop;

import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.model.common.Consumable;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;
import java.util.function.Function;

public interface Prize {
	String getCaptcha();

	String getRealCaptcha();

	void award(User u);

	void award(EventUser u);

	int getPrize();

	Consumable getPrizeAsItem();

	String[] getPrizeWithPenalty();

	Map.Entry<String, Function<User, Boolean>> getRequirement();

	Map.Entry<String, Function<EventUser, Boolean>> getRequirementForTwitch();
}
